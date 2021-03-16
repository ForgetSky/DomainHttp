# DomainHttp

基于OkHttp分装的一个库，用来适配多个接口域名的场景，通过自定义拦截器，实现根据域名的不同，分别执行不同的请求处理流程；
一个域名对应一个处理策略，在各自的处理策略中实现对请求数据的处理；相互独立，互不影响；

## 支持的功能有哪些？

- 添加公共请求头
- 添加公共请求参数
- 接口数据的加解密
- 密钥协商
- 接口重试
- DNS缓存
- 请求日志打印
- 接口请求流程监控

## 有哪些优点？

- 只需要简单配置即可接入使用，非常适用于多域名，接口需要加密的场景。配置灵活，使用简单。
- 数据加解密，密钥协商，接口重试，这些让人头疼的处理流程都已经封装好，只需要实现对应的几个方法就可以了
- 侵入性小，只要项目中使用的是OkHttp,就可以使用该库
- 只需要创建使用一个OkHttpClient，内存占用少

## Gradle

```groovy
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
    
	dependencies {
	        implementation 'com.github.ForgetSky:DomainHttp:1.0.0'
	}
```

## Usage

 ```kt
 private fun okHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(DomainPolicyInterceptor()) //多域名策略拦截器
            .addInterceptor(DnsRetryInterceptor()) //Dns重试
            .addInterceptor(loggingInterceptor()) //日志拦截器
            .eventListenerFactory(getEventListenerFactory()) //请求过程监控
            .dns(CacheDns())   //Dns缓存
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
两种方式，选择一个自己方便使用的就行；

- 怎么实现数据加解密和握手流程？
  都在BaseEncryptPolicy封装好了，只需要继承它，实现抽象方法就行了；
  不需要加密的环境，实现IDomainPolicy就行
  
- 怎么使用DNS优化功能？
CacheDns()和 DnsRetryInterceptor()

详细使用实例请查看demo
