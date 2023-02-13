# CustomizablePaginationInterceptor
  #####                                 MybatisPlus.HuanyuMake
---
###### Compare with the PaginationInnerInterceptor, the Interceptor supports user to custom the SQL to query total records of you other SQL result

###### 与Mybatis-plus官方提供的内置分页插件 PaginnationInnerInterceptor 相比，该拦截器支持用户自定义用来查总记录数的sql语句
`CustomizablePaginationInterceptor (Alias: Customizable)`
`PaginnationInnerInterceptor  (Alias: Inner)`    

`Customizable` 继承 `Inner`
`Customizable`只是对原生`Inner`的拓展,使用`Customizable`插件**不会**给原项目带来额外负担

## 在 application.yaml 中的设置
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
 ## 其它说明
1. 以上 `customizable-pagination-interceptor` 下的属性都不是必要项, 都有默认值, 按需设置就行
  
2. 如果不设置 `open-mapper-count-sql`为 `true`, 则不能自动使用自定义sql来查询总记录数
 使用以上配置, 例子:
 ```xml
    mapper.xml

    <select id="selectAllSubscriberById" parameterType="int" resultType="user">
        SELECT subscriber.*
        FROM blogger, subscriber
    </select>

    <!-- 设置了open-mapper-count-sql = true , 且 count-sql-suffix = '_COUNT' 则会使用该句查询总记录数 -->
    <!-- 这是自定义 selectAllSubscriberById sql的总数查询sql的sql语句, 但要注意这些语句的 resultType 必须为 java.lang.Long-->
    <select id="selectAllSubscriberById_COUNT" parameterType="int" resultType="Long">
        SELECT subscriber.id
        FROM blogger, subscriber
    </select>

    <!-- 自定义mybatis-plus中selectPage的总数查询sql -->
    <select id="selectPage_COUNT" resultType="Long">
        select TABLE_ROWS AS total from information_schema.TABLES where TABLE_SCHEMA = 'poetryplatform' AND TABLE_NAME = 'users'
    </select>
 ```
3. pom.xml 概览
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
 
