package pdlab.minecraft.debug;

public class Assert {
	private static final String DEFAULT_ERROR_MESSAGE = "Assertion Failed!";
	
	public static void True(boolean condition) {
		Assert.True(condition, DEFAULT_ERROR_MESSAGE);
	}
	
	public static void True(boolean condition, String errMsg) {
		if(!condition) throw new AssertionError(errMsg);
	}
	
	public static void False(boolean condition) {
		Assert.False(condition, DEFAULT_ERROR_MESSAGE);
	}
	
	public static void False(boolean condition, String errMsg) {
		if(condition) throw new AssertionError(errMsg);
	}
	
	public static void condition(boolean condition) {
		Assert.condition(condition, DEFAULT_ERROR_MESSAGE);
	}
	
	public static void condition(boolean condition, String errMsg) {
		if(!condition) throw new AssertionError(errMsg);
	}
}
