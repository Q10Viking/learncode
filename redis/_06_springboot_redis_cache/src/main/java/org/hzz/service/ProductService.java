package org.hzz.service;

import com.alibaba.fastjson.JSON;
import org.hzz.entity.Product;
import org.hzz.mapper.ProductMapper;
import org.hzz.util.RedisKeyPrefixConst;
import org.hzz.util.RedisUtil;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class ProductService {
    // 缓存的操作时间
    public static final Integer PRODUCT_CACHE_TIMEOUT = 60 * 60 * 24;
    private Logger logger = LoggerFactory.getLogger(ProductService.class);


    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private Redisson redisson;

    @Autowired
    private RedisUtil redisUtil;

    public void createProduct(Product product){
        productMapper.createProduct(product);
    }

    public void updateProduct(Product product){
        productMapper.updateProduct(product);
    }

    /**-------------------------------------------------------------------------------
     *    简单版redis+db
     *-------------------------------------------------------------------------------*/
    public Product getProductV1(Integer id){
        String productKey = RedisKeyPrefixConst.PRODUCT_CACHE+id;
        Product product = null;
        // 从redis中获取
        product = JSON.parseObject(redisUtil.get(productKey), Product.class);
        if(product!=null){
            logger.info("从缓存中获取");
            return product;
        }
        logger.info("从DB获取");
        product = productMapper.selectById(id);

        // 加入缓存 并且有过期时间
        redisUtil.set(productKey,JSON.toJSONString(product),PRODUCT_CACHE_TIMEOUT, TimeUnit.SECONDS);
        return product;
    }


    /**-------------------------------------------------------------------------------
     *    简单版redis+db 冷热分离
     *-------------------------------------------------------------------------------*/
    public Product getProductV2(Integer id){
        String productKey = RedisKeyPrefixConst.PRODUCT_CACHE+id;
        Product product = null;
        // 从redis中获取
        product = JSON.parseObject(redisUtil.get(productKey), Product.class);
        if(product!=null){
            // 冷热分离，延长过期时间
            redisUtil.expire(productKey,genProductCahceTimeout(),TimeUnit.SECONDS);
            logger.info("从缓存中获取，并延长过期时间");
            return product;
        }
        logger.info("从DB获取");
        product = productMapper.selectById(id);

        // 加入缓存 并且有过期时间
        redisUtil.set(productKey,JSON.toJSONString(product),PRODUCT_CACHE_TIMEOUT, TimeUnit.SECONDS);
        return product;
    }


    /**-------------------------------------------------------------------------------
     *    简单版redis+db + 冷热分离 + 缓存击穿（失效）
     *-------------------------------------------------------------------------------*/
    public Product getProductV3(Integer id){
        String productKey = RedisKeyPrefixConst.PRODUCT_CACHE+id;
        Product product = null;
        // 从redis中获取
        product = JSON.parseObject(redisUtil.get(productKey), Product.class);
        if(product!=null){
            // 冷热分离，延长过期时间
            redisUtil.expire(productKey,genProductCahceTimeout(),TimeUnit.SECONDS);
            logger.info("从缓存中获取，并延长过期时间");
            return product;
        }
        logger.info("从DB获取");
        product = productMapper.selectById(id);

        // 加入缓存 并且设置随机的过期时间
        redisUtil.set(productKey,JSON.toJSONString(product),genProductCahceTimeout(), TimeUnit.SECONDS);
        return product;
    }


    /**-------------------------------------------------------------------------------
     *    简单版redis+db + 冷热分离 + 缓存击穿（失效）+ 缓存穿透
     *-------------------------------------------------------------------------------*/
    private static final String EMPTY_CACHE = "{}";
    public Product getProductV4(Integer id){
        String productKey = RedisKeyPrefixConst.PRODUCT_CACHE+id;
        Product product = null;
        // 从redis中获取
        String cacheStr = redisUtil.get(productKey);

        if(cacheStr!=null){
            if(EMPTY_CACHE.equals(cacheStr)){
                logger.info("Redis层处理缓存空对象");
                // 给它延长过期时间
                redisUtil.expire(productKey,genEmptyCacheTimeout(),TimeUnit.SECONDS);
                return new Product();
            }

            product = JSON.parseObject(cacheStr, Product.class);
            // 冷热分离，延长过期时间
            redisUtil.expire(productKey,genProductCahceTimeout(),TimeUnit.SECONDS);
            logger.info("从缓存中获取，并延长过期时间");
            return product;
        }
        logger.info("从DB获取");
        product = productMapper.selectById(id);
        // 处理缓存穿透放入一个空对象
        if (product == null){
            logger.info("处理缓存穿透");
            redisUtil.set(productKey,EMPTY_CACHE,genEmptyCacheTimeout(),TimeUnit.SECONDS);
        }else{
            // 加入缓存 并且设置随机的过期时间
            redisUtil.set(productKey,JSON.toJSONString(product),genProductCahceTimeout(), TimeUnit.SECONDS);
        }
        return product;
    }



    /**-------------------------------------------------------------------------------
     *    简单版redis+db + 冷热分离 + 缓存击穿（失效）+ 缓存穿透
     *    + 热点数据重建
     *-------------------------------------------------------------------------------*/
    private static final String LOCK_PRODUCT_HOT_CACHE_PREFIX = "lock:product:hot_cache:";
    public Product getProductV5(Integer id){
        String productKey = RedisKeyPrefixConst.PRODUCT_CACHE+id;
        String lockProductKey = LOCK_PRODUCT_HOT_CACHE_PREFIX + id;
        Product product = null;
        product = getFromRedisCache(productKey);
        if(product != null){
            return product;
        }

        // 热点数据重建,可能有多个线程去重建，但是只需要一个线程建立就好
        RLock lock = redisson.getLock(lockProductKey);
        lock.lock();
        try{
            logger.info("热点数据重建，双重检查");
            product = getFromRedisCache(productKey);
            if(product != null){
                return product;
            }
            return getFromDB(id);
        }finally {
            logger.info("释放锁");
            lock.unlock();
        }
    }


    /**-------------------------------------------------------------------------------
     *    简单版redis+db + 冷热分离 + 缓存击穿（失效）+ 缓存穿透
     *    + 热点数据重建 + 锁优化
     *-------------------------------------------------------------------------------*/
    public Product getProductV6(Integer id){
        String productKey = RedisKeyPrefixConst.PRODUCT_CACHE+id;
        String lockProductKey = LOCK_PRODUCT_HOT_CACHE_PREFIX + id;
        Product product = null;
        product = getFromRedisCache(productKey);
        if(product != null){
            return product;
        }

        // 热点数据重建,可能有多个线程去重建，但是只需要一个线程建立就好
        RLock lock = redisson.getLock(lockProductKey);
        // lock.lock();
        try{
            lock.tryLock(1,TimeUnit.SECONDS);
            logger.info("热点数据重建，双重检查");
            product = getFromRedisCache(productKey);
            if(product != null){
                return product;
            }
            return getFromDB(id);
        } catch (InterruptedException e) {
            logger.info("获取锁超时，直接从db获取");
            return getFromDB(id);
        } finally {
            logger.info("释放锁");
            lock.unlock();
        }
    }

    public Product getFromRedisCache(String productKey){
        Product product = null;
        // 从redis中获取
        String cacheStr = redisUtil.get(productKey);
        if(cacheStr!=null){
            if(EMPTY_CACHE.equals(cacheStr)){
                logger.info("Redis层处理缓存空对象");
                // 给它延长过期时间
                redisUtil.expire(productKey,genEmptyCacheTimeout(),TimeUnit.SECONDS);
                return new Product();
            }

            product = JSON.parseObject(cacheStr, Product.class);
            // 冷热分离，延长过期时间
            redisUtil.expire(productKey,genProductCahceTimeout(),TimeUnit.SECONDS);
            logger.info("从缓存中获取，并延长过期时间");
        }
        return product;
    }

    public Product getFromDB(Integer id){
        logger.info("从DB获取");
        String productKey = RedisKeyPrefixConst.PRODUCT_CACHE+id;
        Product product = null;
        product = productMapper.selectById(id);
        // 处理缓存穿透放入一个空对象
        if (product == null){
            logger.info("处理缓存穿透");
            redisUtil.set(productKey,EMPTY_CACHE,genEmptyCacheTimeout(),TimeUnit.SECONDS);
        }else{
            // 加入缓存 并且设置随机的过期时间
            redisUtil.set(productKey,JSON.toJSONString(product),genProductCahceTimeout(), TimeUnit.SECONDS);
        }
        return product;
    }

    public int genEmptyCacheTimeout(){
        return 60 + new Random().nextInt(30);
    }

    public int genProductCahceTimeout(){
        // 在24小时基础上，延长0-4小时
        return PRODUCT_CACHE_TIMEOUT + new Random().nextInt(5)*60*60;
    }
}
