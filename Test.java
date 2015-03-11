class Test {
	public static void main(String[] args) {
		System.out.println("Hello World");
		int a = countInstances(Thread.class);
       	System.out.println("There are " + a + " instances of " + Thread.class);		
       	Object[] objs = getAllInstances(Thread.class, System.nanoTime());
       	System.out.println("Arr Length:" + objs.length);
       	System.out.println("Objects: " + java.util.Arrays.toString(objs));
	}

	private static native int countInstances(Class klass);
	private static native Object[] getAllInstances(Class klass, long tag);
}