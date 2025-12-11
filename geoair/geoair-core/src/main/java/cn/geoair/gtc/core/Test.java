package cn.geoair.gtc.core;

import java.util.HashMap;
import java.util.Map;

import cn.geoair.gtc.base.Gtc;
import cn.geoair.gtc.base.gpa.id.GtcIdGenerator;
import cn.geoair.gtc.base.json.GtcJSON;
import cn.geoair.gtc.base.tool.GkSnowflake;
import cn.geoair.gtc.base.util.GutilBean;
import cn.geoair.gtc.spi.log.Log4Gtc;

/**
 * 测试用
 * @author Administrator
 *
 */
public class Test {



	public static void main(String[] args) {


		//SpringFactoriesLoader.loadFactories(null, null)

		//GkSpLoader.LOADER_CACHE.clear();

	}



	public static void main1(String[] args) {



		//System.out.println(CallerUtil.getCallerName());
		for(int i=0;i<100;i++) {
        	System.out.println( GtcIdGenerator.timestampId());
        }

		//if(1==1) {
		//	return;
		//}
		Student c = new Student("c", 12);

		Student student = new Student();

		GutilBean.copyProperties(c,student,"12");
		System.out.println(student.getName());
		System.out.println(student.getAge());

//		Type[] clss =  gtcGenericTypeUtil.resolveTypeArguments( gtcValueJson.class,  gtcValueVo.class);
//
//
//		 gtc.log.info("{}", clss);

	}


	public static class Student{

		private String name;
		private int age;

		private Class<?> studentTypeClass = Student.class;

		public Student() {

		}
		public Student(String name, int age) {
			super();
			this.name = name;
			this.age = age;
		}

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getAge() {
			return age;
		}
		public void setAge(int age) {
			this.age = age;
		}

		public Class<?> getStudentTypeClass() {
			return studentTypeClass;
		}
		public void setStudentTypeClass(Class<?> studentTypeClass) {
			this.studentTypeClass = studentTypeClass;
		}




	}


	public static void main3(String[] args) {

		Log4Gtc.setLogType(Log4Gtc.LogType.HUTOOL);
		Map<String,Object> map = new HashMap<>();
		map.put("a", 111);
		map.put("b", "df");

		map.put("c", new Student("c",12));

		map.put("d", new Student("d",12));

		 GtcJSON js =  Gtc.toJson(map);
		System.out.println(js.toJSONString());
		 GtcJSON json =  GtcJSON.toJson(js.toJSONString());

		System.out.println(json.getByPath("d", Student.class).getName());

		Student st = new Student("e",14);
		 GtcJSON jsSt =  GtcJSON.toJson(st);
		//Student stu = jsSt.getByPath("",  Student.class);
		Student stu = jsSt.toBean(Student.class,true);
		System.out.println(stu.getAge());

	}


	public static void main2(String[] args) {


		//System.out.println(new Morse().encode("123456qwe"));
		//if(1==1)return;
		/*
		if(1==1)return;

		System.out.println(IdUtil.fastSimpleUUID().toString());
		System.out.println(IdUtil.randomUUID().toString());
		System.out.println(IdUtil.simpleUUID().toString());
		System.out.println(IdUtil.fastUUID().toString());


		System.out.println(IdUtil.nanoId());
		System.out.println(IdUtil.objectId());
		System.out.println(IdUtil.getWorkerId(1,1));

		System.out.println(IdUtil.fastUUID().toString());
		if(1==1)return;
		*/

		long begin = System.currentTimeMillis();


		GkSnowflake snowflake = new GkSnowflake(1,1,false);

		for(int i=0;i<10;i++) {
			long key = snowflake.nextId();
			System.out.println(key);
			String key36 = encode36(key);
			System.out.println(key36);
			//System.out.println(snowflake.getGenerateSequence(key));
			System.out.println(decode36(key36));



			System.out.println();
			//System.out.println(key);
			//System.out.println(System.currentTimeMillis());
			//long times =  snowflake.getGenerateDateTime(key);

			//System.out.println(times);

			//Date data = new Date(times);

			//Calendar calendar = Calendar.getInstance();

			//calendar.setTimeInMillis(times);

			/*
			 StringBuilder sb = new StringBuilder();
		        sb.append(snowflake.getDataCenterId(key)).append(":").append(snowflake.getWorkerId(key)).append(":");
			sb.append(String.valueOf(calendar.get(Calendar.YEAR)).substring(2));
			sb.append(calendar.get(Calendar.MONTH) + 1);
			sb.append(calendar.get(Calendar.DAY_OF_MONTH));
			sb.append(calendar.get(Calendar.HOUR_OF_DAY));
			sb.append(calendar.get(Calendar.MINUTE));
			sb.append(calendar.get(Calendar.SECOND));
			sb.append(calendar.get(Calendar.MILLISECOND));

			sb.append(":");

			sb.append(snowflake.getGenerateSequence(key));
			System.out.println(sb.toString());
			*/
		}
		/*
		for(int i=0;i<100;i++) {
			key = snowflake.nextId();
			//System.out.println(key);//1585181885799039031

			//System.out.println(Long.toHexString(key));
			//System.out.println(Long.toOctalString(key));
			//System.out.println(Long.toBinaryString(key));
			System.out.println(encode(key));
			//System.out.println("-----------------------------------------");
			//System.out.println( gtcIdGenerator.timestampId());//20221026160931974011
			//System.out.println(IdUtil.objectId());//6358eb1f6b48166be281e427
		}
		*/
		System.out.println(System.currentTimeMillis()-begin);


	}



	private static String CHARSTRING = "0123456789abcdefghijklmnopqrstuvwxyz";
	private static char[] CHARS = CHARSTRING.toCharArray();

	/**
	 * 转字符串
	 * @param num
	 * @return
	 */

	public static String encode36(long num) {



        StringBuffer stringBuffer = new StringBuffer();

        if(num == 0) {
            stringBuffer.append(CHARS[0]);
        }

        while(num > 0) {
            stringBuffer.append(CHARS[(int) (num % 36)]);
            num = num / 36;
        }

        return stringBuffer.reverse().toString();
    }


	/**
     * 转数值
     * @param code
     * @return
     */
    public static long decode36(String code) {
        int size = code.length();
        long num = 0;
        for(int i = 0; i<size; i++) {
            //String char2str = String.valueOf(code.charAt(i)).toLowerCase();
            //num = (long) (CHARSMAP.get(char2str.charAt(0)) * Math.pow(36, size - i - 1) + num);


        	num = (long) (CHARSTRING.indexOf(code.charAt(i)) * Math.pow(36, size - i - 1)) + num;
        	//num = (long) (CHARSMAP.get(code.charAt(i)) * Math.pow(36, size - i - 1)) + num;
        }

        return num;
    }

}




