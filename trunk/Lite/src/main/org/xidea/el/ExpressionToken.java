package org.xidea.el;

/**
 * 运算符编号说明：
 * 0<<12 | 0<<8 | 0<<6 | 0<<2 |0
 * 1111          1111          11        1111     11      
 * 二级优先级     二级组内序号   参数标志   优先级  组内序号
 * 
 * 参数标志 说明：
 * 00 一位操作符
 * 01 两位操作符
 * 10 保留标志(三位操作符?)
 * 11 保留标志(零位/四位操作符?)
 * @author jindw
 */
public abstract interface ExpressionToken {
	public static final int BIT_PRIORITY = 15<<2;
	public static final int BIT_PRIORITY_SUB = 15<<12;
	public static final int BIT_PARAM = 3<<6;

	//值类型（<=0）
	//常量标记（String,Number,Boolean,Null）
//	@Deprecated
//	public static final int VALUE_LAZY = -0x00;
	public static final int VALUE_CONSTANTS = -0x01;//c;
	public static final int VALUE_VAR = -0x02;//n;
	public static final int VALUE_NEW_LIST = -0x03;//[;
	public static final int VALUE_NEW_MAP = -0x04;//{;
	
	
	//九：（最高级别的运算符号）
	public static final int OP_GET_PROP        = 0<<12 | 0<<8 | 1<<6 | 8<<2 | 0;
	//0<<5 | 8<<2 |1;//.;
	public static final int OP_GET_STATIC_PROP = 0<<12 | 0<<8 | 0<<6 | 8<<2 | 1;
	//1<<5 | 8<<1 |0;//.#;解析时可以忽略改节点，表达式优化的时候处理
	public static final int OP_INVOKE_METHOD                  = 0<<12 | 0<<8 | 1<<6 | 8<<2 | 2;
	//2<<5 | 8<<1 |1;//()
	public static final int OP_INVOKE_METHOD_WITH_STATIC_PARAM= 0<<12 | 0<<8 | 0<<6 | 8<<2 | 3;
	//3<<5 | 8<<1 |0;//()
	public static final int OP_INVOKE_METHOD_WITH_ONE_PARAM   = 0<<12 | 1<<8 | 1<<6 | 8<<2 | 0;
	//4<<5 | 8<<1 |1;//()
	

	//八：（次高级别）
	public static final int OP_NOT = 0<<12 | 0<<8 | 0<<6 | 7<<2 | 0;
	//0<<5 | 7<<1 |0;//!;
	public static final int OP_POS = 0<<12 | 0<<8 | 0<<6 | 7<<2 | 1;
	//1<<5 | 7<<1 |0;//+;//正数
	public static final int OP_NEG = 0<<12 | 0<<8 | 0<<6 | 7<<2 | 2;
	//2<<5 | 7<<1 |0;//-;//负数
	
	//七：
	public static final int OP_MUL = 0<<12 | 0<<8 | 1<<6 | 6<<2 | 0;
	//0<<5 | 6<<1 |1;//*;
	public static final int OP_DIV = 0<<12 | 0<<8 | 1<<6 | 6<<2 | 1;
	//1<<5 | 6<<1 |1;///;
	public static final int OP_MOD = 0<<12 | 0<<8 | 1<<6 | 6<<2 | 2;
	//2<<5 | 6<<1 |1;//%;
	
	//六：
	//与正负符号共享了字面值
	public static final int OP_ADD = 0<<12 | 0<<8 | 1<<6 | 5<<2 | 0;
	//0<<5 | 5<<1 |1;//+;//6
	public static final int OP_SUB = 0<<12 | 0<<8 | 1<<6 | 5<<2 | 1;
	//1<<5 | 5<<1 |1;//-;
	

	//五
	public static final int OP_LT =    0<<12 | 0<<8 | 1<<6 | 4<<2 | 0;
	//0<<5 | 4<<1 |1;//<;
	public static final int OP_GT =    0<<12 | 0<<8 | 1<<6 | 4<<2 | 1;
	//1<<5 | 4<<1 |1;//>;
	public static final int OP_LTEQ =  0<<12 | 0<<8 | 1<<6 | 4<<2 | 2;
	//2<<5 | 4<<1 |1;//<=;
	public static final int OP_GTEQ =  0<<12 | 0<<8 | 1<<6 | 4<<2 | 3;
	//3<<5 | 4<<1 |1;//>=;
	
	//四:
	public static final int OP_EQ =    0<<12 | 0<<8 | 1<<6 | 3<<2 | 0;
	//4<<5 | 3<<1 |1;//==;
	public static final int OP_NOTEQ = 0<<12 | 0<<8 | 1<<6 | 3<<2 | 1;
	//5<<5 | 3<<1 |1;//!=;

	static final int OP_BIT_AND = 4<<12 | 0<<8 | 1<<6 | 2<<2 | 0;
	static final int OP_BIT_XOR = 3<<12 | 0<<8 | 1<<6 | 2<<2 | 0;
	static final int OP_BIT_OR  = 2<<12 | 0<<8 | 1<<6 | 2<<2 | 0;
	
	
	//三：
	public static final int OP_AND = 1<<12 | 0<<8 | 1<<6 | 2<<2 | 0;
	//0<<5 | 2<<1 |1;//&&;
	public static final int OP_OR =  0<<12 | 0<<8 | 1<<6 | 2<<2 | 1;
	//1<<5 | 2<<1 |1;//||;

	//二：
	public static final int OP_QUESTION =        0<<12 | 0<<8 | 1<<6 | 1<<2 | 0;
	//0<<5 | 1<<1 |1;//?;
	public static final int OP_QUESTION_SELECT = 0<<12 | 0<<8 | 1<<6 | 1<<2 | 1;
	//1<<5 | 1<<1 |1;//:;

	//一：
	//与Map Join 共享字面量（map join 会忽略）
	public static final int OP_PARAM_JOIN = 0<<12 | 0<<8 | 1<<6 | 0<<2 | 0;
	//0<<5 | 0<<1 |1;//,
	//与三元运算符共享字面值
	public static final int OP_MAP_PUSH = 0<<12 | 0<<8 | 1<<6 | 0<<2 | 1;
	//1<<5 | 0<<1 |1;//:,
	

	
	
	public int getType();
	public String toString();
	public ExpressionToken getLeft();
	public ExpressionToken getRight();
	public Object getParam();
}
