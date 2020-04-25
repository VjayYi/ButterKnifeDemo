package my.test.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import my.test.annotation.BindView;
import my.test.annotation.OnClick;

/**
 * Created by YiVjay
 * on 2020/4/25
 */

@AutoService(Processor.class)
//@SupportedAnnotationTypes()//要处理的注解 和重写getSupportedAnnotationTypes方法效果一样
@SupportedSourceVersion(SourceVersion.RELEASE_7)//什么版本的JDK进行编译
public class ButterknifeProcessor extends AbstractProcessor {
    private Messager messager;//不要打印ERROR会直接结束
    private Elements elementsUtils;
    private Filer filer;
    private Types typesUtils;

    List<TypeElement> activityNames;//含有所要遍历注解的类的集合

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementsUtils = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();

        activityNames = new ArrayList<>();
        messager.printMessage(Diagnostic.Kind.NOTE, "init---------------------");
    }

    //可能调用多次但只有一次有数据
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        messager.printMessage(Diagnostic.Kind.NOTE, "process--------------------");
        activityNames.clear();

        //遍历BindView注解字段并存入map
        Set<? extends Element> bindViewSet = roundEnv.getElementsAnnotatedWith(BindView.class);
        Map<String, List<VariableElement>> bindViewMap = new HashMap<>();
        for (Element element : bindViewSet) {

            VariableElement variableElement = (VariableElement) element;
            String activityName = getActivityName(variableElement);
            if (!bindViewMap.containsKey(activityName)) {
                bindViewMap.put(activityName, new ArrayList<VariableElement>());
            }
            bindViewMap.get(activityName).add(variableElement);

        }
        //遍历OnClick注解方法并存入map
        Set<? extends Element> clickViewSet = roundEnv.getElementsAnnotatedWith(OnClick.class);
        Map<String, List<ExecutableElement>> clickViewMap = new HashMap<>();
        for (Element element : clickViewSet) {

            ExecutableElement executableElement = (ExecutableElement) element;
            String activityName = getActivityName(executableElement);
            if (!clickViewMap.containsKey(activityName)) {
                clickViewMap.put(activityName, new ArrayList<ExecutableElement>());
            }
            clickViewMap.get(activityName).add(executableElement);

        }

        //遍历含有注解的类的集合生成代码(以下简称注解类)
        for (TypeElement activityName : activityNames) {
            //包名
            String packageName = activityName.getQualifiedName().toString().replace(
                    "." + activityName.getSimpleName().toString(), "");
            //注解类全名
            String qualifiedName = activityName.getQualifiedName().toString();
            //注解类简称
            String simpleName = activityName.getSimpleName().toString();
            //生成类简称
            String newSimpleName = simpleName + "$ViewBinder";


            messager.printMessage(Diagnostic.Kind.NOTE, "start--------------------" +  newSimpleName);

            //注解类
            ClassName className = ClassName.get(packageName, simpleName);

            //生成类方法
            MethodSpec.Builder bind = MethodSpec.methodBuilder("bind")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addParameter(className, "target", Modifier.FINAL)
                    .addAnnotation(Override.class);

            //遍历此类中注解BindView的属性，生成findViewById代码
            for (VariableElement variableElement : bindViewMap.get(qualifiedName)) {
                //属性名称
                String fieldName = variableElement.getSimpleName().toString();
                //注解value
                int annValue = variableElement.getAnnotation(BindView.class).value();
                //属性类型
                TypeName fileType = ClassName.get(variableElement.asType());
                //写入代码
                bind.addStatement("$N." + fieldName + "=($T)$N.findViewById($L)", "target", fileType, "target", annValue);

            }
            //遍历此类中注解OnClick的属性，生成setOnClickListener代码
            for (ExecutableElement executableElement : clickViewMap.get(qualifiedName)) {
                //方法名称
                String fieldName = executableElement.getSimpleName().toString();
                //注解value
                int[] annValue = executableElement.getAnnotation(OnClick.class).value();
                //自定义点击事件类
                ClassName clicklistener = ClassName.get("my.test.apt_pro", "ReOnClickListener");
                //View类
                ClassName view = ClassName.get("android.view", "View");
                //生成匿名内部类
                TypeSpec bindm = TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(clicklistener)
                        .addMethod(MethodSpec.methodBuilder("doClick")
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .addParameter(view, "v")
                                .returns(void.class)
                                .addStatement(" $N.$N($N)", "target", fieldName, "v")
                                .build())
                        .build();
                bind.addStatement("$L $N = $L",clicklistener,"clicklistener",bindm);


                //写入代码
                for (int i : annValue) {
                    bind.addStatement("$N.findViewById($L).setOnClickListener($N)", "target", i, "clicklistener");
                }

            }
            //所要实现的接口
            ClassName interfaceName = ClassName.get("my.test.apt_pro", "ViewBinder");
            //实现接口<泛型>
            ParameterizedTypeName typeName = ParameterizedTypeName.get(interfaceName, className);
            //写类
            TypeSpec typeBuild = TypeSpec.classBuilder(newSimpleName)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(typeName)
                    .addMethod(bind.build())
                    .build();


            JavaFile javaFile = JavaFile.builder(packageName, typeBuild)
                    .build();


            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {

                e.printStackTrace();
            }

        }


        return false;
    }

    /**
     * 找到元素所在的类 并加入集合
     *
     * @param element
     * @return
     */
    private String getActivityName(Element element) {
        Element e = element;
        while (!e.getKind().equals(ElementKind.CLASS)) {
            e = e.getEnclosingElement();
        }
        TypeElement te = (TypeElement) e;

        if (!activityNames.contains(te)) {
            activityNames.add(te);
            messager.printMessage(Diagnostic.Kind.NOTE, "activity--------" + te.getQualifiedName().toString());
        }
        return te.getQualifiedName().toString();
    }

    /**
     * 所要遍历的注解
     *
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(BindView.class.getCanonicalName());
        types.add(OnClick.class.getCanonicalName());
        return types;
    }
}
