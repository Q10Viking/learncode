package main

func main() {
	hi := func1("静默")
	println(hi)

	age, name := func2("静默", 18)
	println(age, name)

	age1, name1 := func3("静默", 18)
	println(age1, name1)

	func4("Java", "Golang")

	func5("静默", "Java programming", "Go programming")
}

// fun1 只有一个返回值，不需要括号括起来
func func1(name string) string {
	return "Hi " + name
}

//  返回值没有名字
func func2(name string, age int) (int, string) {
	return age + 1, "Hi" + name
}

//  返回值具有名字，可以在内部直接复制，然后返回
func func3(name string, age int) (ageAlias int, nameAlias string) {
	ageAlias = age + 1
	nameAlias = "Hi " + name
	return
}

// 多个参数具有相同类型放在一起，可以只写一次类型
func func4(java, golang string) {
	println("静默 leanring " + java + " " + golang)
}

// 不定参数要放在最后面
func func5(name string, books ...string) {
	println(name + " learning")
	for _, book := range books {
		println(book)
	}
}
