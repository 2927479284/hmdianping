package com.hmdp;

class Father {
    int x = 10;
    public Father() {
        System.out.println("Father");
        this.print();
        this.x = 20;
    }
    public void print(){
        System.out.println("Father.x="+x);
    }
}

class Son extends Father{
    int x = 30;
    public Son() {
        super();
        System.out.println("son");
        this.print();
        this.x = 40;
    }
    public void print(){
        System.out.println("son.x="+x);
    }
}

class B{
    int a = 10;

    public B() {

    }
    public void print(){
        System.out.println(a);
    }

}
class A{
    public static void main(String[] args) {
        Father father = new Son();
        System.out.println(father.x);
    }
}


 class Servlet {
    public void service() {
        System.out.println("Servlet.service()");
        doGet();
    }

    public void doGet() {
        System.out.println("Servlet.doGet()");
    }

    public static void main(String[] args) {
        Servlet s = new MyServlet();
        s.service();
    }
}

class MyServlet extends Servlet {
    public MyServlet() {
        //自动添加一个方法
        super();
        System.out.println("MyServlet.service()");
    }

    public void doGet() {
        System.out.println("MyServlet.doGet()");
    }
}

