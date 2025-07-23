# bean-model - 让直接 new 的对象也能自动依赖注入

bean-model 旨在让你在直接通过 `new` 创建对象时，也能像 Spring 容器管理的 Bean 一样，自动为其所有需要依赖注入的属性（如 `@Autowired`、`@Resource` 标注的字段）完成注入。适用于自定义工厂、异步线程等非 Spring 容器管理的场景。

## 功能特性

- **Spring 容器外依赖注入**：无需通过 Spring 容器获取 Bean，直接 new 出来的对象也能自动注入依赖。
- **支持多种注入方式**：支持 `@Autowired`、`@Resource` 注解，兼容集合、Map 等复杂类型的自动注入。
- **简单易用**：只需在启动类上添加 `@ModelScan` 注解并指定需要扫描的包，模型类实现 `IModel<T>` 接口或继承 `ModelUp` 抽象类，即可享受自动注入能力。

## 快速开始

### 1. 在启动类上添加 @ModelScan

```java
@ModelScan("com.model.**")
public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
}
```

### 2. 定义模型类

#### 方式一：实现接口

```java
class ModelDemo1 implements IModel<ModelDemo1> {
    @Autowired
    private DemoService demoService;
}
```

#### 方式二：继承抽象类

```java
class ModelDemo2 extends ModelUp {
    @Resource
    private DemoService demoService;
}
```

### 3. 使用效果

```java
public static void main(String[] args) {
    // 接口式用法，调用 model() 后 demoService 会被自动注入
    ModelDemo1 modelDemo1 = new ModelDemo1().model();
    modelDemo1.demoService.doSomething();

    // 继承式用法，直接 new 出来 demoService 也会被自动注入
    ModelDemo2 modelDemo2 = new ModelDemo2();
    modelDemo2.demoService.doSomething();
}
```

## 适用场景
- 需要在 Spring 容器外部（如多线程、异步任务、自定义工厂等）创建对象，并希望这些对象的依赖依然能够自动注入。