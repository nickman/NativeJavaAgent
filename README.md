# NativeJavaAgent
The NativeJavaAgent is a JVMTI library supported by a Java library that allows you to:
* Get a count of the number of objects in the heap of a specific type or a type and any type that inherit/implement from that type.
* Acquire an object array of all the objects in the heap of a specific type or a type and any type that inherit/implement from that type.

### Instance Counts

Here's an  simple example of getting instance counts:

```java
	import com.heliosapm.jvmti.agent.Agent;
    
    public static void main(String[] args) {
		
		final Agent agent = Agent.getInstance();
        System.gc();  // a lot of garbage created at boot time
		final long startTime = System.currentTimeMillis();
		
		final int objectCount = agent.getInstanceCountOf(Object.class);
		log("Object instance count:%s", objectCount);
		
		final int charSequenceCount = agent.getInstanceCountOfAny(CharSequence.class);
		log("CharSequence instance count:%s", charSequenceCount);
		
		log("Elapsed:%s ms.", System.currentTimeMillis()-startTime);
	}
```

The output is:

```
Initializing Agent OnAttach...
Agent Initialized
Object instance count:467
CharSequence instance count:3196
Elapsed:3 ms.
```

What's going on here:

1. Acquire the (singleton) agent using `com.heliosapm.jvmti.agent.Agent.getInstance()`. In this case, the underlying native JVMTI library was loaded at runtime. The library can be loaded at boot time using `-agentpath:oif_agent.so` or if not loaded on boot, it will be loaded dynamically when the agent is first called.
2. Calling `agent.getInstanceCountOf(Object.class)` returns the number of Object instances found in the heap. The `getInstanceCountOf` method only counts objects of the exact type so it will not count instances of any type inherrited from `java.lang.Object` (basically everything else).
3. On the other hamd, in the next call, `agent.getInstanceCountOfAny(CharSequence.class)`, counts all instances in the heap that extend `java.lang.CharSequence`.
4. Since there's not much else going on in the JVM, the number of object instances is fairly small, but agent is fairly quick. Even so, warmup makes a difference. If I add this code to the example above:

```java
		int total = 0;
		for(int i = 0; i < 100; i++) {
			final int oc = agent.getInstanceCountOf(Object.class);
			final int cc = agent.getInstanceCountOfAny(CharSequence.class);
			total += (oc + cc);
			System.gc();
		}

		final long startTime2 = System.currentTimeMillis();
		
		final int objectCount2 = agent.getInstanceCountOf(Object.class);
		log("Object instance count:%s", objectCount2);
		
		final int charSequenceCount2 = agent.getInstanceCountOfAny(CharSequence.class);
		log("CharSequence instance count:%s", charSequenceCount2);
		
		log("Elapsed:%s ms.", System.currentTimeMillis()-startTime2);

```

.... then the output of the second timing is:

```
Object instance count:450
CharSequence instance count:3174
Elapsed:1 ms.
```

Note that background activity and the agent itself generate some number of objects, so for "accurate" counts, I am calling System.gc() at specific points so we're not counting unreachable but uncleared objects.

### Instance References

In this example, the agent acquires the actual references to the first 20 `java.lang.String` instances found on the heap. The maximum number of instances supplied as 20 is optional. If not supplied, it will default to `Integer.MAX_VALUE`. It then prints a selection of those strings so we can see examples of the sort of strings hanging out in the heap.

```java
	public static void main(String[] args) {
		final Agent agent = Agent.getInstance();
		System.gc();
		String[] strings = agent.getInstancesOf(String.class, 20);
		for(int i = 10; i < 15; i++) {
			log("String #%s: [%s]", i, strings[i]);
		}
		strings = null;  // Don't prevent gc of these objects !
	}

```
Your output will vary, but in my last test I saw:

```
Initializing Agent OnAttach...
Agent Initialized
Aborting Instance Tagging after 20 Instances
String #10: [Unexpected vector type encounterd: entry_offset = ]
String #11: [ (0x]
String #12: [), units = ]
String #13: [Unexpected variability attribute: entry_offset = 0x]
String #14: [ name = ]
```

As with the counting calls, the instance calls come in 2 flavours where `getInstancesOf` only retrieves objects of the exact supplied type, whereas the `getInstancesOfAny` retrieves instances of the specified type or any type that inherrits/extends that type. Repeating the same example for `CharSequence`s:

```java
		CharSequence[] charSeqs = agent.getInstancesOfAny(CharSequence.class, 20);
		for(int i = 10; i < 15; i++) {
			log("CharSequence#%s: Type:%s, Value:[%s]", 
            i, charSeqs[i].getClass().getName(), charSeqs[i].toString());
		}
		charSeqs = null;
```

The output:

```
CharSequence#10: Type:java.lang.String, Value:[%(\d+\$)?([-#+ 0,(\<]*)?(\d+)?(\.\d+)?([tT])?([a-zA-Z%])]
CharSequence#11: Type:java.lang.String, Value:[DISPLAY]
CharSequence#12: Type:java.lang.String, Value:[user.language.display]
CharSequence#13: Type:java.lang.String, Value:[user.script.display]
CharSequence#14: Type:java.lang.String, Value:[user.country.display]
```
