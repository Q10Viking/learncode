package org.hzz;

import org.hzz.pojo.Emp;
import org.junit.Test;

import static org.junit.Assert.*;

import java.sql.*;

public class JDBCTest {

    @Test
    public void testjdbc() throws ClassNotFoundException, SQLException {
        // ============================SqlSessionFactory======================================
        // 1.注册驱动
        Class.forName("com.mysql.cj.jdbc.Driver");
        // 2.创建数据连接 DriverManager.getConnection()方法：获取数据库连接
        String url = "jdbc:mysql://192.168.187.135:3306/learn_mybatis";
        String user = "root";
        String password = "Root.123456";
        // 3.获取连接
        Connection connection = DriverManager.getConnection(url, user, password);
        String sql = "SELECT * FROM emp WHERE id=?";

        // ============================SqlSession======================================
        // 4.使用PreparedStatement 预解析sql语句，
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, 1);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Emp emp = new Emp();
            emp.setId(rs.getInt("id"));
            emp.setUsername(rs.getString("username"));
            System.out.println(emp);
            assertEquals("q10viking", emp.getUsername());
        }
    }
}
