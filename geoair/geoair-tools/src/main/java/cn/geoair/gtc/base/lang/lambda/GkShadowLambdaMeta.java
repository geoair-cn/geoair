package cn.geoair.gtc.base.lang.lambda;

import cn.geoair.gtc.base.exception.GtcException;
import cn.geoair.gtc.base.util.GutilClass;

/**
 * 基于 {@link GkSerializedLambda} 创建的元信息
 *
 */
public class GkShadowLambdaMeta implements GkfLambdaMeta {
    private final GkSerializedLambda lambda;

    public GkShadowLambdaMeta(GkSerializedLambda lambda) {
        this.lambda = lambda;
    }

    @Override
    public String getImplMethodName() {
        return lambda.getImplMethodName();
    }

    @Override
    public Class<?> getInstantiatedClass() {
        String instantiatedMethodType = lambda.getInstantiatedMethodType();
        String instantiatedType = instantiatedMethodType.substring(2, instantiatedMethodType.indexOf(';')).replace('/', '.');
        try {
			return GutilClass.forName(instantiatedType, lambda.getCapturingClass().getClassLoader());
		} catch (ClassNotFoundException | LinkageError e) {
			throw new GtcException("",e);
		}
    }

	@Override
	public Class<?> getCapturingClass() {

		return lambda.getCapturingClass();
	}

	@Override
	public String getFunctionalInterfaceClass() {

		return lambda.getFunctionalInterfaceClass();
	}

	@Override
	public String getFunctionalInterfaceMethodName() {

		return lambda.getFunctionalInterfaceMethodName();
	}

	@Override
	public String getFunctionalInterfaceMethodSignature() {

		return lambda.getFunctionalInterfaceMethodSignature();
	}

	@Override
	public String getImplClass() {

		return lambda.getImplClass();
	}

	@Override
	public String getImplMethodSignature() {

		return lambda.getImplMethodSignature();
	}

	@Override
	public int getImplMethodKind() {

		return lambda.getImplMethodKind();
	}

	@Override
	public String getInstantiatedMethodType() {

		return lambda.getInstantiatedMethodType();
	}

	@Override
	public int getCapturedArgCount() {

		return lambda.getCapturedArgCount();
	}

	@Override
	public Object getCapturedArg(int i) {

		return lambda.getCapturedArg(i);
	}

}
