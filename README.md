# CustomizablePaginationInterceptor
  #####                                 MybatisPlus.HuanyuMake
---
###### Compare with the PaginationInnerInterceptor, the Interceptor supports user to custom the SQL to query total records of you other SQL result

###### 与Mybatis-plus官方提供的内置分页插件 PaginnationInnerInterceptor 相比，该拦截器支持用户自定义用来查总记录数的sql语句
`CustomizablePaginationInterceptor (Alias: Customizable)`
`PaginnationInnerInterceptor  (Alias: Inner)`    

`Customizable` 继承 `Inner`
`Customizable`只是对原生`Inner`的拓展,使用`Customizable`插件**不会**给原项目带来额外负担

## 1.在 application.yaml 中的设置
```yaml
mybatis-plus:
  type-aliases-package: org.HuanyuMake.pojo 
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
    cache-enabled: true
    # 以下设置 CustomizablePaginationInterceptor Bean的属性
    customizable-pagination-interceptor:
      count-field: 'COUNT(1)'   # 自定义 select countField from (xx) 的countField默认内容, 默认为 'COUNT(1)'
      count-field-alias: 'total' # 自定义 select countField AS countFieldAlias from (xx) countFieldAlias 内容, 默认为 'total'
      open-mapper-count-sql: true  # 开启对同mapper中 总记录查询语句的<select>字句的使用, 默认为 false
      count-sql-suffix: '_COUNT' # 选择使用什么后缀的同id<select>子句作为总记录数查询sql, 默认为'_COUNT'
      max-limit: 2000  # 自定义一页最多有多少条记录数
      
 ```
 ---
 ## 2.在项目目录中放置 `CustomizablePaginationInterceptor.java` 源文件,记得改包名
 ```java
 package com.yourCom.project.mybatisPlus.Interceptor;

public class CustomizablePaginationInterceptor extends PaginationInnerInterceptor {...}
 ```
 ---
 ## 3.注册 `CustomizablePaginationInterceptor` 插件
 ```java
@Configuration
@Getter
@Setter
public class MybatisPlusConfig {

    private final CustomizablePaginationInterceptor cpi;
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        interceptor.addInnerInterceptor(cpi);
        return interceptor;
    }

    @Autowired
    public MybatisPlusConfig(CustomizablePaginationInterceptor cpi) {
        this.cpi = cpi;
    }
}
 ```
 ---
 ## 4.在mapper.xml中配置对应自定义查询sql,这里引用 `其它说明` 的 `第3点` 的例子
 
 ## 5.调用mapper接口方法, 这里调用的是 selectPage(new Page<>(1,10),null)

 ## 5.控制台日志
 ```cmd
 ==>  Preparing: select TABLE_ROWS AS total from information_schema.TABLES where TABLE_SCHEMA = 'project' AND TABLE_NAME = 'users'
==> Parameters: 
<==    Columns: total
<==        Row: 12
<==      Total: 1
==>  Preparing: SELECT id,name,login_state,latest_login_time FROM users LIMIT ?
==> Parameters: 10(Long)
 ```
 ---
 ## 其它说明
1. 以上 `customizable-pagination-interceptor` 下的属性都不是必要项, 都有默认值, 按需设置就行

2. 不注册插件的话, `CustomizablePaginationInterceptor`是不会工作的
  
3. 如果不设置 `open-mapper-count-sql`为 `true`, 则不能自动使用自定义sql来查询总记录数
 使用以上配置, 例子:
 ```xml
    mapper.xml

    <!--这是支持 selectPage(Page<T>) 分页方法查询总数的sql-->
   <select id="selectAllSubscriberById" parameterType="int" resultType="user">
       SELECT subscriber.*
       FROM blogger, subscriber
   </select>

   <!-- 设置了open-mapper-count-sql = true , 且 count-sql-suffix = '_COUNT' 则会使用该句查询总记录数 -->
   <!-- 这是自定义 selectAllSubscriberById sql的总数查询sql的sql语句, 但要注意这些语句的 resultType 必须为 java.lang.Long-->
   <select id="selectAllSubscriberById_COUNT" parameterType="int" resultType="Long">
       SELECT COUNT(subscriber.id)
       FROM blogger, subscriber
   </select>

   <!-- 自定义mybatis-plus中selectPage的总数查询sql -->
   <select id="selectPage_COUNT" resultType="Long">
       select TABLE_ROWS AS total from information_schema.TABLES where TABLE_SCHEMA = 'project' AND TABLE_NAME = 'users'
   </select>
 ```
4. pom.xml 概览
 ```xml
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <!-- 注意! 我这里使用的是SpringBoot3 -->
        <version>3.0.2</version>  
        <relativePath/>
    </parent>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.1</version>
        </dependency>
      
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <!-- 注意! 我使用的是3.x新版本的mybatis-plus-->
            <version>3.5.3.1</version> 
        </dependency>
    </dependencies>
```
5. 版本不匹配咋办?    
  A: 笔者没试过SpringBoot2 和 1 (初学者一枚), 这个不是很清楚． 依赖的 `PaginnationInnerInterceptor` 类好像在  mybatis-plus-boot-starter 2.x的老版本时好像不叫这个名字, 下载源码后直接修改使用即可
