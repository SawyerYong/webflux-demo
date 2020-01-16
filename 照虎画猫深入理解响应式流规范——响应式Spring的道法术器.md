# 2 响应式编程之法

上一章在响应式编程库方面，本着“快速上手”的原则，介绍了响应式流的概念，以及Reactor 3的使用。这一章，我们基于对Reactor 3源码的模仿，从《响应式流规范》入手，深入了解响应式流开发库。

## 2.1 响应式流规范

现代软件对近乎实时地处理数据的需求越来越强烈，对不断变化的信息的即时响应，意味着更大的商业价值，流处理是一种快速将数据转换为有用信息的手段。

数据流中的元素可以是一个一个的待计算的数据，也可以是一个一个待响应的事件。前者多用于大数据处理，比如Storm、Spark等产品，后者常用于响应式编程，比如Netflix在使用的RxJava、Scala编程语言的发明者Typesafe公司（已更名为Lightbend）的Akka Stream、Java开发者都熟悉的Pivotal公司的Project Reactor、走在技术前沿的Vert.x等。

软件行业是一个非常注重分享和交流的行业。随着对响应式编程技术的讨论与沟通逐渐深入，2013年末的时候，Netflix、Pivotal、Typesafe等公司的工程师们共同发起了关于制定**“响应式流规范（Reactive Stream Specification）”**的倡议和讨论，并在github上创建了[reactive-streams-jvm](https://github.com/reactive-streams/reactive-streams-jvm)项目。到2015年5月份，1.0版本的规范出炉，项目README就是规范正文。

各个响应式开发库都要遵循这个规范，其好处也是显而易见的。之所以我们编写的Java代码可以在Hotspot、J9和Zing等JVM运行，是因为它们都遵循Java虚拟机规范。类似的，由于各个响应式开发库都遵循响应式流规范，因此互相兼容，不同的开发库之间可以进行交互，我们甚至可以同时在项目中使用多个响应式开发库。对于Spring WebFlux来说，也可以使用RxJava作为响应式库。

<img height=300em src="https://leanote.com/api/file/getImage?fileId=5a9e3ed2ab644159cf00128e"/>;

虽然响应式流规范是用来约束响应式开发库的，作为使用者的我们如果能够了解这一规范对于我们理解开发库的使用也是很有帮助的，因为规范的内容都是对响应式编程思想的精髓的呈现。访问[reactive-streams-jvm](https://github.com/reactive-streams/reactive-streams-jvm)项目，可以浏览规范的细节，包括其中定义的响应式流的特点：

1. 具有处理无限数量的元素的能力；
2. 按序处理；
3. 异步地传递元素；
4. 必须实现非阻塞的回压（backpressure）。

### 2.1.1 响应式流接口

响应式流规范定义了四个接口，如下：

1.`Publisher`是能够发出元素的发布者。

```
public interface Publisher<T> {
    public void subscribe(Subscriber<? super T> s);
}
```

2.`Subscriber`是接收元素并做出响应的订阅者。

```
public interface Subscriber<T> {
    public void onSubscribe(Subscription s);
    public void onNext(T t);
    public void onError(Throwable t);
    public void onComplete();
}
```

当执行`subscribe`方法时，发布者会回调订阅者的`onSubscribe`方法，这个方法中，通常订阅者会借助传入的`Subscription`向发布者请求n个数据。然后发布者通过不断调用订阅者的`onNext`方法向订阅者发出最多n个数据。如果数据全部发完，则会调用`onComplete`告知订阅者流已经发完；如果有错误发生，则通过`onError`发出错误数据，同样也会终止流。

![title](https://leanote.com/api/file/getImage?fileId=5a9e9425ab644157e0001da0)

订阅后的回调用表达式表示就是`onSubscribe onNext* (onError | onComplete)?`，即以一个`onSubscribe`开始，中间有0个或多个`onNext`，最后有0个或1个`onError`或`onComplete`事件。

`Publisher`和`Subscriber`融合了迭代器模式和观察者模式。

我们经常用到的`Iterable`和`Iterator`就是迭代器模式的体现，可以满足上边第1和2个特点关于按需处理数据流的要求；而观察者模式基于事件的回调机制有助于满足第3个特点关于异步传递元素的要求。

3.`Subscription`是`Publisher`和`Subscriber`的“中间人”。

```
public interface Subscription {
    public void request(long n);
    public void cancel();
}
```

当发布者调用`subscribe`方法注册订阅者时，会通过订阅者的回调方法`onSubscribe`传入`Subscription`对象，之后订阅者就可以使用这个`Subscription`对象的`request`方法向发布者“要”数据了。回压机制正是基于此来实现的，因此第4个特点也能够实现了。

4.`Processor`集`Publisher`和`Subscriber`于一身。

```
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
```

这四个接口在[JEP 266](http://openjdk.java.net/jeps/266)跟随Java 9版本被[引入了Java SDK](http://www.reactive-streams.org/)。

这四个接口是实现各开发库之间互相兼容的桥梁，响应式流规范也仅仅聚焦于此，而对诸如转换、合并、分组等等的操作一概未做要求，因此是一个非常抽象且精简的接口规范。

如果这时候有人要造轮子，再写一套响应式开发库，如何基于这几个接口展开呢？

### 2.1.2 照虎画猫，理解订阅后发生了什么

Reactor 3是遵循响应式流规范的实现，因此，小撸一把Reactor的源码有助于我们理解规范中定义的接口的使用。

Reactor中，我们最先接触的生成`Publisher`的方法就是`Flux.just()`，下面我们来动手写代码模拟一下Reactor的实现方式。不过具备生产能力开发库会考虑性能、并发安全性等诸多因素，所谓“照虎画猫”，我们的代码只是模拟出实现思路，代码少的多，但五脏俱全。

> 源码位于：https://github.com/get-set/get-reactive/tree/master/my-reactor

首先，引入响应式流规范的四个接口定义，基于Java 9的话可以直接使用[java.util.concurrent.Flow](https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/Flow.html)：

```
    <dependency>
        <groupId>org.reactivestreams</groupId>
        <artifactId>reactive-streams</artifactId>
        <version>1.0.2</version>
    </dependency>
```

首先创建最最基础的类`Flux`，它是一个`Publisher`。

```
package reactor.core.publisher;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public abstract class Flux<T> implements Publisher<T> {
    public abstract void subscribe(Subscriber<? super T> s);
}
```

在Reactor中，`Flux`既是一个发布者，又充当工具类的角色，当我们用`Flux.just()`、`Flux.range()`或`Flux.interval()`等工厂方法生成`Flux`时，会new一个新的`Flux`，比如`Flux.just`会返回一个`FluxArray`对象。

```
    public static <T> Flux<T> just(T... data) {
        return new FluxArray<>(data);
    }
```

返回的`FluxArray`对象是`Flux.just`生成的`Publisher`，它继承自`Flux`，并实现了`subscribe`方法。

```
public class FluxArray<T> extends Flux<T> {
    private T[] array;  // 1

    public FluxArray(T[] data) {
        this.array = data;
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        actual.onSubscribe(new ArraySubscription<>(actual, array)); // 2
    }
}
```

1. `FluxArray`内部使用一个数组来保存数据；
2. `subscribe`方法通常会回调`Subscriber`的`onSubscribe`方法，该方法需要传入一个`Subscription`对象，从而订阅者之后可以通过回调传回的`Subscription`的`request`方法跟`FluxArray`请求数据，这也是回压的应有之义。

继续编写`ArraySubscription`：

```
public class FluxArray<T> extends Flux<T> {
    ...
    static class ArraySubscription<T> implements Subscription { // 1
        final Subscriber<? super T> actual;
        final T[] array;    // 2
        int index;
        boolean canceled;

        public ArraySubscription(Subscriber<? super T> actual, T[] array) {
            this.actual = actual;
            this.array = array;
        }

        @Override
        public void request(long n) {
            if (canceled) {
                return;
            }
            long length = array.length;
            for (int i = 0; i < n && index < length; i++) {
                actual.onNext(array[index++]);  // 3
            }
            if (index == length) {
                actual.onComplete();    // 4
            }
        }

        @Override
        public void cancel() {  // 5
            this.canceled = true;
        }
    }
}
```

1. `ArraySubscription`是一个静态内部类。静态内部类是最简单的一种内部类，你尽可以把它当成普通的类，只不过恰好定义在其他类的内部；
2. 可见在`Subscription`内也有一份数据；
3. 当有可以发出的元素时，回调订阅者的`onNext`方法传递元素；
4. 当所有的元素都发完时，回调订阅者的`onComplete`方法；
5. 订阅者可以使用`Subscription`取消订阅。

到此为止，发布者就开发完了。我们测试一下：

```
@Test
public void fluxArrayTest() {
    Flux.just(1, 2, 3, 4, 5).subscribe(new Subscriber<Integer>() { // 1

        @Override
        public void onSubscribe(Subscription s) {
            System.out.println("onSubscribe");
            s.request(6);   // 2
        }

        @Override
        public void onNext(Integer integer) {
            System.out.println("onNext:" + integer);
        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void onComplete() {
            System.out.println("onComplete");
        }
    });
}
```

1. `Subscriber`通过匿名内部类定义，其中需要实现接口的四个方法；
2. 订阅时请求6个元素。

测试方法运行如下：

```
1
2
3
4
5
Completed.
```

如果请求3个元素呢？输出如下：

```
1
2
3
```

没有完成事件，OK，一个简单的`Flux.just`就完成了，通过这个例子我们能够初步摸出`Flux`工厂方法的一些“套路”：

- 工厂方法返回的是`Flux`子类的实例，如`FluxArray`；
- `FluxArray`的`subscribe`方法会返回给订阅者一个`Subscription`实现类的对象，这个`ArraySubscription`是`FluxArray`的静态内部类，定义了“如何发布元素”的逻辑；
- 订阅者可以通过这个`ArraySubscription`对象向发布者请求n个数据；发布者也可以借助这个`ArraySubscription`对象向订阅者传递数据元素（onNext/onError/onComplete）。

用图来表示如下（由于Subscription是静态内部类，可以看做普通类，就单独放一边了）：

![title](https://leanote.com/api/file/getImage?fileId=5aa0e236ab64411766000d01)

上图的这个过程基本适用于大多数的用于生成`Flux`/`Mono`的静态工厂方法，如`Flux.just`、`Flux.range`等。

首先，使用类似`Flux.just`的方法创建发布者后，会创建一个具体的发布者（`Publisher`），如`FluxArray`。

1. 当使用`.subscribe`订阅这个发布者时，首先会new一个具有相应逻辑的`Subscription`（如`ArraySubscription`，这个`Subscription`定义了如何处理下游的`request`，以及如何“发出数据”）；
2. 然后发布者将这个`Subscription`通过订阅者的`.onSubscribe`方法传给订阅者；
3. 在订阅者的`.onSubscribe`方法中，需要通过`Subscription`发起第一次的请求`.request`；
4. `Subscription`收到请求，就可以通过回调订阅者的`onNext`方法发出元素了，有多少发多少，但不能超过请求的个数；
5. 订阅者在`onNext`中通常定义对元素的处理逻辑，处理完成之后，可以继续发起请求；
6. 发布者根据继续满足订阅者的请求；
7. 直至发布者的序列结束，通过订阅者的`onComplete`予以告知；当然序列发送过程中如果有错误，则通过订阅者的`onError`予以告知并传递错误信息；这两种情况都会导致序列终止，订阅过程结束。

以上从1~7这些阶段称为**订阅期（subscribe time）**。

### 2.1.3 照虎画猫——操作符“流水线”

响应式开发库的一个很赞的特性就是可以像组装流水线一样将操作符串起来，用来声明复杂的处理逻辑。比如：

```
Flux ff = Flux.just(1, 2, 3, 4, 5)
    .map(i -> i * i)
    .filter(i -> (i % 2) == 0);
ff.subscribe(...)
```

通过源码，我们可以了解这种“流水线”的实现机制。下面我们仍然是通过照虎画猫的方式模拟一下Reactor中`Flux.map`的实现方式。

`Flux.map`用于实现转换，转换后元素的类型可能会发生变化，转换的逻辑由参数`Function`决定。方法本身返回的是一个转换后的`Flux`，基于此，该方法实现如下：

```
public abstract class Flux<T> implements Publisher<T> {
    ...
    public <V> Flux<V> map(Function<? super T, ? extends V> mapper) {   // 1
        return new FluxMap<>(this, mapper); // 2
    }
}
```

1. 泛型方法，通过泛型表示可能出现的类型的变化（T → V）；
2. `FluxMap`就是新的Flux。

既然`FluxMap`是一个新的Flux，那么与2.1.2中`FluxArray`类似，其内部定义有`MapSubscription`，这是一个`Subscription`，能够根据其订阅者的请求发出数据。

```
public class FluxMap<T, R> extends Flux<R> {

    private final Flux<? extends T> source;
    private final Function<? super T, ? extends R> mapper;

    public FluxMap(Flux<? extends T> source, Function<? super T, ? extends R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public void subscribe(Subscriber<? super R> actual) {
        source.subscribe(new MapSubscriber<>(actual, mapper));
    }

    static final class MapSubscription<T, R> implements Subscription {
        private final Subscriber<? super R> actual;
        private final Function<? super T, ? extends R> mapper;

        MapSubscriber(Subscriber<? super R> actual, Function<? super T, ? extends R> mapper) {
            this.actual = actual;
            this.mapper = mapper;
        }

        @Override
        public void request(long n) {   // 1
            // TODO 收到请求，发出元素
        }

        @Override
        public void cancel() {
            // TODO 取消订阅
        }
    }
}
```

1. 但是`map`操作符并不产生数据，只是数据的搬运工。收到`request`后要发出的数据来自上游。

所以`MapSubscription`同时也应该是一个订阅者，它订阅上游的发布者，并将数据处理后传递给下游的订阅者（为了跟Reactor源码一致，将`MapSubscription`改名为`MapSubscriber`，其实没差）。

![title](https://leanote.com/api/file/getImage?fileId=5aa122e7ab6441156c0014af)

如图，**对下游是作为发布者，传递上游的数据到下游；对上游是作为订阅者，传递下游的请求到上游。**

```
static final class MapSubscriber<T, R> implements Subscriber<T>, Subscription { // 1
    ...
}
```

1. 实现了`Subscriber`和`Subscription`。

这样，总共有5个方法要实现：来自`Subscriber`接口的`onSubscribe`、`onNext`、`onError`、`onComplete`，和来自`Subscription`接口的`request`和`cancel`。下面我们本着“搬运工”的角色实现这几个方法即可。

```
static final class MapSubscriber<T, R> implements Subscriber<T>, Subscription {
    private final Subscriber<? super R> actual;
    private final Function<? super T, ? extends R> mapper;

    boolean done;

    Subscription subscriptionOfUpstream;

    MapSubscriber(Subscriber<? super R> actual, Function<? super T, ? extends R> mapper) {
        this.actual = actual;
        this.mapper = mapper;
    }

    @Override
    public void onSubscribe(Subscription s) {
        this.subscriptionOfUpstream = s;    // 1
        actual.onSubscribe(this);           // 2
    }

    @Override
    public void onNext(T t) {
        if (done) {
            return;
        }
        actual.onNext(mapper.apply(t));     // 3
    }

    @Override
    public void onError(Throwable t) { 
        if (done) {
            return;
        }
        done = true;
        actual.onError(t);                  // 4
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;
        actual.onComplete();                // 5
    }

    @Override
    public void request(long n) {
        this.subscriptionOfUpstream.request(n);     // 6
    }

    @Override
    public void cancel() {
        this.subscriptionOfUpstream.cancel();       // 7
    }
}
```

1. 拿到来自上游的Subscription；
2. 回调下游的`onSubscribe`，将自身作为`Subscription`传递过去；
3. 收到上游发出的数据后，将其用mapper进行转换，然后接着发给下游；
4. 将上游的错误信号原样发给下游；
5. 将上游的完成信号原样发给下游；
6. 将下游的请求传递给上游；
7. 将下游的取消操作传递给上游。

从这个对源码的模仿，可以体会到，当有多个操作符串成“操作链”的时候：

- 向下：很自然地，数据和信号（`onSubscribe`、`onNext`、`onError`、`onComplete`）是通过每一个操作符向下传递的，传递的过程中进行相应的操作处理，这一点并不难理解；
- 向上：然而在内部我们看不到的是，有一个自下而上的“订阅链”，这个订阅链可以用来传递`request`，因此回压（backpressure）可以实现从下游向上游的传递。

这一节最开头的那一段代码的执行过程如下图所示：

![title](https://leanote.com/api/file/getImage?fileId=5aa1e5f0ab644172e500045b)

### 2.1.4 LambdaSubscriber

在1.3.2节的时候，介绍了`.subscribe`的几个不同方法签名的变种：

```java
subscribe(  Consumer<? super T> consumer) 

subscribe(  @Nullable Consumer<? super T> consumer, 
            Consumer<? super Throwable> errorConsumer) 

subscribe(  @Nullable Consumer<? super T> consumer,
            @Nullable Consumer<? super Throwable> errorConsumer,
            @Nullable Runnable completeConsumer) 

subscribe(  @Nullable Consumer<? super T> consumer,
            @Nullable Consumer<? super Throwable> errorConsumer,
            @Nullable Runnable completeConsumer,
            @Nullable Consumer<? super Subscription> subscriptionConsumer)
```

用起来非常方便，但是响应式流规范中只定义了一个订阅方法`subscribe(Subscriber subscriber)`。实际上，这几个方法最终都是调用的`subscribe(LambdaSubscriber subscriber)`，并通过`LambdaSubscriber`实现了对不同个数参数的组装。如下图所示：

![title](https://leanote.com/api/file/getImage?fileId=5aa3cca9ab6441500f00115e)

因此，

```java
    flux.subscribe(System.out::println, System.err::println);
```

是调用的：

```java
    flux.subscribe(new LambdaSubscriber(System.out::println, System.err::println, null, null));
```