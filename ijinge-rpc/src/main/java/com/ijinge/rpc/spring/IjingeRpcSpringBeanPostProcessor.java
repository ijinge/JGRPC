package com.ijinge.rpc.spring;

import com.ijinge.rpc.annotation.EnableRpc;
import com.ijinge.rpc.annotation.IjingeReference;
import com.ijinge.rpc.annotation.IjingeService;
import com.ijinge.rpc.config.RpcConfig;
import com.ijinge.rpc.factory.SingletonFactory;
import com.ijinge.rpc.netty.NettyClient;
import com.ijinge.rpc.proxy.IjingeRpcClientProxy;
import com.ijinge.rpc.register.NacosTemplate;
import com.ijinge.rpc.server.IjingeServiceProvider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Slf4j
@Component
public class IjingeRpcSpringBeanPostProcessor implements BeanPostProcessor, BeanFactoryPostProcessor {

    private final IjingeServiceProvider serviceProvider;
    private NettyClient nettyClient;
    private NacosTemplate nacosTemplate;
    private RpcConfig rpcConfig;

    public IjingeRpcSpringBeanPostProcessor() {
        serviceProvider =  SingletonFactory.getInstance(IjingeServiceProvider.class);
        //创建netty客户端
        nettyClient = SingletonFactory.getInstance(NettyClient.class);
        nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
    }
    //bean初始化方法调用后被调用 (一般在代理包装写在初始化方法之后)
    @SneakyThrows
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //在这里判断bean上有没有加ijingeService注解
        //如果有，将其发布为服务
        if (bean.getClass().isAnnotationPresent(IjingeService.class)){
            IjingeService ijingeService = bean.getClass().getAnnotation(IjingeService.class);
            serviceProvider.publishService(ijingeService,bean);
        }
        //在这里判断bean里面的字段有没有加@ijingeRefrence注解
        //如果有 识别并生成代理实现类，发起网络请求
        Class<?> targetClass = bean.getClass();
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            IjingeReference annotation = declaredField.getAnnotation(IjingeReference.class);
            if (annotation != null){
                //代理实现类，调用方法的时候 会触发invoke方法，在其中实现网络调用
                IjingeRpcClientProxy ijingeRpcClientProxy = new IjingeRpcClientProxy(annotation,nettyClient);
                Object proxy = ijingeRpcClientProxy.getProxy(declaredField.getType());
                //当isAccessible()的结果是false时不允许通过反射访问该字段
                declaredField.setAccessible(true);
                try {
                    // 这里的bean其实就是Controller ,这里是对Controller中的 service注入proxy对象
                    declaredField.set(bean,proxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }

    // bean初始化方法前被调用
    @SneakyThrows
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        EnableRpc enableRpc = bean.getClass().getAnnotation(EnableRpc.class);
        if (enableRpc != null){
            if (rpcConfig == null){
                log.info("EnableRpc会先于所有的bean初始化之前执行，在这里我们进行配置的加载");
                rpcConfig = new RpcConfig();
                rpcConfig.setNacosGroup(enableRpc.nacosGroup());
                rpcConfig.setNacosHost(enableRpc.nacosHost());
                rpcConfig.setNacosPort(enableRpc.nacosPort());
                rpcConfig.setProviderPort(enableRpc.serverPort());
                serviceProvider.init(rpcConfig);
                //nacos 根据配置进行初始化
                nacosTemplate.init(rpcConfig.getNacosHost(),rpcConfig.getNacosPort());
                //客户端加载配置
                nettyClient.setRpcConfig(rpcConfig);
            }
        }
        return bean;
    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                // init scanner
                Class<?> scannerClass = ClassUtils.forName ( "org.springframework.context.annotation.ClassPathBeanDefinitionScanner",
                        IjingeRpcSpringBeanPostProcessor.class.getClassLoader () );
                Object scanner = scannerClass.getConstructor ( new Class<?>[]{BeanDefinitionRegistry.class, boolean.class} )
                        .newInstance ( new Object[]{(BeanDefinitionRegistry) beanFactory, true} );
                // add filter
                Class<?> filterClass = ClassUtils.forName ( "org.springframework.core.type.filter.AnnotationTypeFilter",
                        IjingeRpcSpringBeanPostProcessor.class.getClassLoader () );
                Object filter = filterClass.getConstructor ( Class.class ).newInstance ( EnableRpc.class );
                Method addIncludeFilter = scannerClass.getMethod ( "addIncludeFilter",
                        ClassUtils.forName ( "org.springframework.core.type.filter.TypeFilter", IjingeRpcSpringBeanPostProcessor.class.getClassLoader () ) );
                addIncludeFilter.invoke ( scanner, filter );
                // scan packages
                Method scan = scannerClass.getMethod ( "scan", new Class<?>[]{String[].class} );
                String[] packages = new String[]{"com.ijinge.rpc.annotation"};
                scan.invoke(scanner, (Object) packages);
            } catch (Throwable e) {
                log.error(e.getMessage(),e);
            }
        }
    }
}
