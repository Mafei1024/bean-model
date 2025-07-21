# bean-model - 模型与实例隔离
使指定的模型类与工厂实例隔离，并且能自动为模型中的实例填充

## 用法

在启动器上加上@ModelScan，并指定需要扫描的包
``` java 
    @ModelScan("com.model.**")
    public static void main(String[] args) {
        SpringApplication.run(demo.class, args);
    }
```
实现方法上的两种方式
#### 接口使用法

```java
class ModelDemo1 implements IModel<ModelDemo1> {
    @Autowired
    private Demo demo;
}
```
#### 继承使用法使用法
```java
class ModelDemo2 extends ModelUp {
    @Autowired
    private Demo demo;
}
```
#### 使用效果

```java
public static void main(String[] args) {
    // 接口式使用,调用model之后，Demo会被自动填充
    ModelDemo1 modelDemo1 = new ModelDemo1().model();
    // 继承式使用，直接new出来之后，Demo会被自动填充
    ModelDemo2 modelDemo2 = new ModelDemo2();
}
```
