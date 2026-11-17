// ============================================================
//  【怎么运行这个文件】
//    1) javac EmployeeSystemDemo.java   ← 编译，生成 .class 文件
//    2) java  EmployeeSystemDemo        ← 运行
//    也可以一步跑完（JDK 11+）：java EmployeeSystemDemo.java
//
//    如果中文显示成乱码，改用这两行：
//       chcp 65001
//       java "-Dstdout.encoding=UTF-8" EmployeeSystemDemo.java
//    （-D 那个参数一定要加双引号）
//
//  【文件名规定】
//    这个文件必须叫 EmployeeSystemDemo.java。
//    因为下面有一个 public class 就叫这个名字，Java 要求两者一致。
//    一个文件只能有一个 public 类，但可以再放几个"没有 public"的类
//    （下面 Employee、EmployeeContainer 就是这种）。
// ============================================================


import java.util.ArrayList;    // 可变长的数组（相当于 JS 的 []）
import java.util.Collections;  // 工具类，里面有排序方法 sort()
import java.util.List;         // "列表"的规范；ArrayList 是它的一种具体实现


// ============================================================
// 1. Employee —— 员工
// ============================================================
// 后面要给它排序，但 Java 不知道"两个员工谁该排前面"，
// 所以让它实现 Comparable<Employee>，意思是"我自带比较大小的能力"，
// 具体规则写在下面的 compareTo 方法里。
class Employee implements Comparable<Employee> {

    private String name;   // 姓名。private 表示只有本类能直接改，外部要用下面的方法读
    private int id;        // 工号，排序就是按它排

    // 构造器：每次 new Employee("小明", 101) 时自动执行，用来给字段赋值
    public Employee(String name, int id) {
        this.name = name;   // this.name 指"这个对象自己的字段"，右边的 name 是传进来的参数
        this.id = id;
    }

    public String getName() { return name; }   // 提供方法让外部能读到 name
    public int getId() { return id; }

    // 比较规则：返回负数 = 我排前面；返回 0 = 一样大；返回正数 = 我排后面
    // 这里写的是"谁的工号小谁在前"
    // 用 Integer.compare 而不是 this.id - other.id，是因为相减在数字很大时可能算错
    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.id, other.id);
    }

    // 决定"这个对象被打印出来时显示什么"。
    // 不写这个方法，打印出来会是 Employee@1b6d3586 这种看不懂的地址
    @Override
    public String toString() {
        return "Employee{name=" + name + ", id=" + id + "}";
    }
}


// ============================================================
// 2. EmployeeContainer —— 装员工的容器
// ============================================================
// 尖括号里的 T 是个"占位类型"：写代码时先不确定装什么，
// 用的时候才决定。比如 new EmployeeContainer<Employee>() 之后，T 就等于 Employee。
//
// T extends Comparable<T> 是对 T 加限制：只允许放"能比较大小"的类型。
// 为什么加？因为下面要排序（sort），排不了没法比较的东西。不加这个限制，代码编译不过。
class EmployeeContainer<T extends Comparable<T>> {

    private List<T> items = new ArrayList<>();   // 一个只装 T 的列表

    public void add(T item) {                    // 参数只能传 T 类型，传错类型编译就报错
        items.add(item);
    } 

    public void sortAndPrint() {
        Collections.sort(items);                 // 因为上面限制了 T 能比较，这里才能直接排序
        System.out.println("--- 排序后的员工列表 ---");
        for (T item : items) {                   // 依次取出每个元素，相当于 JS 的 for (const x of items)
            System.out.println(item);            // 打印时会自动调用 item.toString()
        }
    }
}


// ============================================================
// 3. EmployeeSystemDemo —— 主程序
// ============================================================
public class EmployeeSystemDemo {

    // 方法自己声明了一个占位类型 <E>（写在返回值 void 的前面，这是固定写法）。
    // 好处是调用时不用手写类型：传进 List<Employee>，它自动就知道 E 是 Employee。
    public static <E> void printListInfo(List<E> list, String desc) {
        System.out.println("[" + desc + "] 列表大小: " + list.size());
    }

    // 参数写成 List<? super Employee>，意思是"元素是 Employee 或它父类的列表"。
    // 因为这里只往里面放东西、不往外取，所以类型可以放宽：
    // 传 List<Employee> 可以，传 List<Object> 也可以。
    // 记法：只往里放的用 super，只往外取的用 extends。
    public static void addIntern(List<? super Employee> list) {
        list.add(new Employee("实习生小王", 999));
    }

    // main 是程序入口，JVM 从这里开始执行。这个签名一个字都不能改。
    public static void main(String[] args) {

        // 第 1 步：创建一个装 Employee 的容器，并放进去 3 个人
        // 右边的 <> 是简写，编译器会自动推断出这里要填 Employee
        EmployeeContainer<Employee> container = new EmployeeContainer<>();
        container.add(new Employee("正式员工老李", 102));
        container.add(new Employee("外包员工老赵", 101));
        container.add(new Employee("实习生小王", 999));

        // 第 2 步：新建一个空列表，打印它的大小 → 0
        // 注意这个 empList 是全新的，和上面 container 里那 3 个人没关系
        List<Employee> empList = new ArrayList<>();
        printListInfo(empList, "初始员工列表");

        // 第 3 步：把实习生加进 empList，再打印 → 大小变成 1
        addIntern(empList);
        printListInfo(empList, "添加实习生后");
        System.out.println("当前列表内容: " + empList);

        // 第 4 步：排序并打印 → 按工号从小到大：101、102、999
        container.sortAndPrint();
    }
}
