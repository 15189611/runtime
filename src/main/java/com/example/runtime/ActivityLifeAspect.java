package com.example.runtime;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.example.annotations.ActivityAutoStart;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Author: Charles.pan
 * Version V1.0
 * Date: 2020/7/13
 * Description:
 * Modification History:
 * Date Author Version Description
 * -----------------------------------------------------------------------------------
 * 2020/7/13 Charles.pan 1.0
 * Why & What is modified:
 */
@Aspect
public class ActivityLifeAspect {
    private static final String TAG = ActivityLifeAspect.class.getSimpleName();
    private static volatile boolean enabled = true;

    @Pointcut("execution(* *(..)) && withinAnnotatedClass()")
    public void methodInsideAnnotatedType() {
    }

    @Pointcut("execution(*.new(..)) && withinAnnotatedClass()")
    public void constructorInsideAnnotatedType() {
    }

    @Pointcut("execution(@com.example.annotations.ActivityAutoStart * *(..))")
    public void lifeAnnotation() {
    }

    public static void setEnabled(boolean enabled) {
        ActivityLifeAspect.enabled = enabled;
    }

    @Around("lifeAnnotation()")
    public Object jumpActivity(ProceedingJoinPoint joinPoint) throws Throwable {
        //生命周期方法执行前
        enterMethod(joinPoint);

        //生命周期方法原体
        Object result = joinPoint.proceed();

        //生命周期方法执行后
        exitMethod(joinPoint);

        return result;
    }

    private void enterMethod(JoinPoint joinPoint) {
        Activity activity = (Activity) joinPoint.getTarget();
        String currentActName = activity.getClass().getCanonicalName();
        Log.e(TAG, "current activity name = " + currentActName);
    }

    private void exitMethod(JoinPoint joinPoint) {
        jumpActivity(joinPoint);
    }

    private void jumpActivity(JoinPoint joinPoint) {
        ActivityAutoStart annotation = getAnnotation(joinPoint, ActivityAutoStart.class);
        if (annotation != null) {
            String activityName = annotation.value();
            Context context = getContext(joinPoint.getThis());
            if (context != null) {
                String packageName = context.getPackageName();
                Log.e(TAG, "need to jump activity full packageName = " + packageName + "." + activityName);
                ComponentName componentName = new ComponentName(packageName, packageName + "." + activityName);
                Intent intent = new Intent();
                intent.setComponent(componentName);
                context.startActivity(intent);
            } else {
                throw new NullPointerException("context is null");
            }
        }
    }

    private <T extends Annotation> T getAnnotation(JoinPoint joinPoint, Class<T> annotationClass) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(annotationClass);
    }

    /**
     * 通过对象获取上下文
     *
     * @param object
     * @return
     */
    private Context getContext(Object object) {
        if (object instanceof Activity) {
            return (Activity) object;
        } else if (object instanceof Fragment) {
            Fragment fragment = (Fragment) object;
            return fragment.getActivity();
        } else if (object instanceof View) {
            View view = (View) object;
            return view.getContext();
        }
        return null;
    }


}
