package springx.beans.support;

import lombok.extern.slf4j.Slf4j;
import springx.beans.config.BeanDefinitionX;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
public class BeanDefinitionReaderX {


    private Properties config = new Properties();

    public BeanDefinitionReaderX() {
    }


    //扫描包(最基层)
    private final String SCAN_PACKAGE = "scanPackage";

    //所有扫描出来的类
    private List<String> scanClass = new ArrayList<>();

    //BeanDefinition(扫描出来的类解析后封装的实体)容器
    private List<BeanDefinitionX> beanDefinitionXES = new ArrayList<>();


    /**
     * 读取配置文件
     *
     * @param locations
     */
    public BeanDefinitionReaderX(String... locations) {
        //通过URL定位找到其所对应的文件，转化为文件流  //getResourceAsStream 从resource下开始计算
        String local = locations[0];
        if (local.contains("classpath:")) local = local.replace("classpath:", "");
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(local);

        try {
            config.load(inputStream);

        } catch (Exception e) {
            log.error(e.toString());
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                log.error(e.toString());
            }
        }

        //自动扫描
        doScanner(config.getProperty(SCAN_PACKAGE));

    }


    //扫描类记录起来
    private void doScanner(String scanPackage) {
        //递归进行扫描
        if (scanPackage.contains("classpath:")) scanPackage = scanPackage.replace("classpath:", "");

        URL url = this.getClass().getClassLoader().getResource(scanPackage);
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "/" + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) continue;
                String className = (scanPackage + "." + file.getName().replaceAll(".class", ""));
                scanClass.add(className);
            }
        }


    }

    public Properties getConfig() {
        return this.config;
    }


    public List<BeanDefinitionX> loadBeanDefinitions(String... locations) {
        for (String className : scanClass) {
            String realClassName = className.replaceAll("/", ".");

            BeanDefinitionX beanDefinitionX = doCreateBeanDefinition(realClassName);

            if (beanDefinitionX!=null)beanDefinitionXES.add(beanDefinitionX);

        }
        return beanDefinitionXES;
    }

    private BeanDefinitionX doCreateBeanDefinition(String className) {
        BeanDefinitionX beanDefinitionX = new BeanDefinitionX();

        try {

            Class<?> clazz = Class.forName(className);
            if (clazz.isInterface()){
                clazz.getMethods();
                return null;
            }

            if (Modifier.isAbstract(clazz.getModifiers())) return null;

            //如果是接口就使用其实现类(不是接口，不是抽象类)
            beanDefinitionX.setBeanClassName(className);
            beanDefinitionX.setFactoryBeanName(lowerFirstCase(clazz.getSimpleName()));
            beanDefinitionX.setLazyInit(false);
            //
            Class<?>[] interfaces = clazz.getInterfaces();
            //for (Class<?> c:interfaces){
            //    //TODO
            //    String interfaceName = c.getName();
            //    beanDefinitionX.setBeanClassName(interfaceName);
            //    beanDefinitionX.setFactoryBeanName(lowerFirstCase(c.getSimpleName()));
            //    beanDefinitionX.setLazyInit(false);
            //}

        } catch (Exception e) {
            log.error(e.toString());
        }


        return beanDefinitionX;
    }

    public static String lowerFirstCase(String simpleName) {

        String first = simpleName.substring(0, 1).toLowerCase();
        String other = simpleName.substring(1);

        return first + other;
    }


}
