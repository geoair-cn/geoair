package cn.geoair.gtc.base.lang.lambda;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import cn.geoair.gtc.base.util.GutilReflection;

/**
 * 在 IDEA 的 Evaluate 中执行的 Lambda 表达式元数据需要使用该类处理元数据
 */
public class GkIdeaProxyLambdaMeta implements GkfLambdaMeta {
    private static final Field FIELD_MEMBER_NAME;
    private static final Field FIELD_MEMBER_NAME_CLAZZ;
    private static final Field FIELD_MEMBER_NAME_NAME;

    static {
        try {
            Class<?> classDirectMethodHandle = Class.forName("java.lang.invoke.DirectMethodHandle");
            FIELD_MEMBER_NAME = GutilReflection.setAccessible(classDirectMethodHandle.getDeclaredField("member"));
            Class<?> classMemberName = Class.forName("java.lang.invoke.MemberName");
            FIELD_MEMBER_NAME_CLAZZ = GutilReflection.setAccessible(classMemberName.getDeclaredField("clazz"));
            FIELD_MEMBER_NAME_NAME = GutilReflection.setAccessible(classMemberName.getDeclaredField("name"));
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private final Class<?> clazz;
    private final String name;

    public GkIdeaProxyLambdaMeta(Proxy func) {
        InvocationHandler handler = Proxy.getInvocationHandler(func);
        try {
            Object dmh = GutilReflection.setAccessible(handler.getClass().getDeclaredField("val$target")).get(handler);
            Object member = FIELD_MEMBER_NAME.get(dmh);
            clazz = (Class<?>) FIELD_MEMBER_NAME_CLAZZ.get(member);
            name = (String) FIELD_MEMBER_NAME_NAME.get(member);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getImplMethodName() {
        return name;
    }

    @Override
    public Class<?> getInstantiatedClass() {
        return clazz;
    }

    @Override
    public String toString() {
        return clazz.getSimpleName() + "::" + name;
    }

	@Override
	public Class<?> getCapturingClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFunctionalInterfaceClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFunctionalInterfaceMethodName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFunctionalInterfaceMethodSignature() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getImplClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getImplMethodSignature() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getImplMethodKind() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getInstantiatedMethodType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCapturedArgCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object getCapturedArg(int i) {
		// TODO Auto-generated method stub
		return null;
	}

}
