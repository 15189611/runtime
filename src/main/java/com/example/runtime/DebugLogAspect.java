package com.example.runtime;

import android.os.Looper;
import android.util.Log;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.concurrent.TimeUnit;

@Aspect
public class DebugLogAspect {
    private static volatile boolean enabled = true;
    private static final String TAG = DebugLogAspect.class.getSimpleName();

    @Pointcut("within(@com.example.annotations.DebugLog *)")
    public void withinAnnotatedClass() {
    }

    @Pointcut("execution(!synthetic * *(..)) && withinAnnotatedClass()")
    public void methodInsideAnnotatedType() {}

    @Pointcut("execution(!synthetic *.new(..)) && withinAnnotatedClass()")
    public void constructorInsideAnnotatedType() {}

    // 语法：execution(@注解 访问权限 返回值的类型 包名.函数名(参数))
    // 表示：使用DebugLog注解的任意类型返回值任意方法名（任意参数）
    @Pointcut("execution(@com.example.annotations.DebugLog * *(..)) || methodInsideAnnotatedType()")
    public void method() {
    }

    @Pointcut("execution(@com.example.annotations.DebugLog *.new(..)) || constructorInsideAnnotatedType()")
    public void constructor() {
    }

    public static void setEnabled(boolean enabled) {
        DebugLogAspect.enabled = enabled;
    }

    //Advance(通知)
    //Advance比较常用的有：Before():方法执行前,After():方法执行后,Around():代替原有逻辑
    @Around("method() || constructor()")
    public Object logAndExecute(ProceedingJoinPoint joinPoint) throws Throwable {
        Log.e(TAG,"LogExecute method is start");
        enterMethod(joinPoint); //原方法之前加入逻辑
        long startNanos = System.nanoTime();
        //执行原方法体
        Object result = joinPoint.proceed();
        long stopNanos = System.nanoTime();
        long lengthMillis = TimeUnit.NANOSECONDS.toMillis(stopNanos - startNanos);

        exitMethod(joinPoint, result, lengthMillis);  //原方法执行后，加逻辑
        return result;
    }

    private static void enterMethod(JoinPoint joinPoint) {
        if(!enabled){
            return ;
        }

        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        System.out.println("方法签名=" + codeSignature);
        Class<?> cls = codeSignature.getDeclaringType();
        System.out.println("方法类型=" + cls);
        String methodName = codeSignature.getName();
        System.out.println("方法名称=" + methodName);
        //方法的参数 变量名称
        String[] parameterNames = codeSignature.getParameterNames();
        System.out.println("方法参数的变量名称=" + parameterNames[0]);
        //方法参数实际 传入的参数值
        Object[] parameterValues = joinPoint.getArgs();
        System.out.println("方法参数的实际传入值=" + parameterValues[0]);

        StringBuilder builder = new StringBuilder("\u21E2 ");
        builder.append(methodName).append('(');
        for (int i = 0; i < parameterValues.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(parameterNames[i]).append('=');
            builder.append(MyStrings.toString(parameterValues[i]));

        }
        builder.append(')');

        if (Looper.myLooper() != Looper.getMainLooper()) {
            builder.append(" [Thread:\"").append(Thread.currentThread().getName()).append("\"]");
        }

        Log.v(asTag(cls), builder.toString());
    }

    private static void exitMethod(JoinPoint joinPoint, Object result, long lengthMillis) {
        if(!enabled){
            return ;
        }

        Signature signature = joinPoint.getSignature();

        Class<?> cls = signature.getDeclaringType();
        String methodName = signature.getName();
        boolean hasReturnType = signature instanceof MethodSignature
                && ((MethodSignature) signature).getReturnType() != void.class;

        StringBuilder builder = new StringBuilder("\u21E0 ")
                .append(methodName)
                .append(" [")
                .append(lengthMillis)
                .append("ms]");

        if (hasReturnType) {
            builder.append(" = ");
            builder.append(MyStrings.toString(result));
        }

        Log.v(asTag(cls), builder.toString());
    }

    private static String asTag(Class<?> cls) {
        if (cls.isAnonymousClass()) {
            return asTag(cls.getEnclosingClass());
        }
        return cls.getSimpleName();
    }
}
