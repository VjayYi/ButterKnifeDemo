package my.test.apt_pro;

/**
 * Created by YiVjay
 * on 2020/4/25
 */
public class Butterknife {
    //通过反射获取ViewBinder子类并执行ViewBinder的bind方法
    public static void bind(Object o) {
        String className = o.getClass().getName() + "$ViewBinder";
        try {
            Class<?> clazz = Class.forName(className);
            ViewBinder viewBinder = (ViewBinder) clazz.newInstance();
            viewBinder.bind(o);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}