# DomainHttp
## 是什么？
基于OkHttp分装的一个库，用来适配多个接口域名的场景，通过自定义拦截器，实现根据域名的不同，分别执行不同的请求处理流程；
一个域名对应一个处理策略，在各自的处理策略中实现对请求数据的处理；相互独立，互不影响；

## 支持的功能有哪些？
- 添加公共请求头
- 添加功能请求参数
- 接口数据的加解密
- 密钥协商
- 接口重试
- DNS缓存优化
- 请求日志打印
- 结束请求流程监听

## 有哪些优点？
- 只需要简单配置即可接入使用，非常适用于多域名，接口需要加密的场景。
- 数据加解密，密钥协商，接口重试，这些让人头疼的处理流程都已经封装好，只需要实现对应的几个方法就可以使用
- 侵入性小，只要项目中使用的是OkHttp,就可以使用该库
- 只使用一个OkHttpClient，性能好

## 怎么用？
 ```
 private fun okHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(DomainPolicyInterceptor())
            .addInterceptor(DnsRetryInterceptor())
            .addInterceptor(loggingInterceptor())
            .eventListenerFactory(getEventListenerFactory())
            .dns(CacheDns())
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
```
- 多域名支持：拦截器DomainPolicyInterceptor，
怎么区分不同域名，从而执行不同的策略呢？有两种方式： 
1. 在接口上添加一个请求头, 这个请求头只会用在本地处理逻辑中，实际网络数据中不会携带发出去，例如在第二套域名策略的所有接口中都加上(damian :test2)
2. 在request中添加一个Tag, 例如：addTag(DomainTag(test2）)
两种方式，选择一个自己方便的使用就行；
- 怎么实现数据加解密和握手流程？
  都在BaseEncryptPolicy封装好了，只需要继承它，实现抽象方法就行了；
  不需要加密的环境，比较简单，实现IDomainPolicy就行
- 怎么使用DNS优化功能？
CacheDns()和 DnsRetryInterceptor()
- 日志打印？ loggingInterceptor()
