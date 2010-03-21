package org.xidea.el;

/**
 * @author jindw
 */
public abstract interface ExpressionToken {

	//值类型（<=0）
	//常量标记（String,Number,Boolean,Null）
	public static final int VALUE_CONSTANTS = -0x0;//c;
	public static final int VALUE_VAR = -0x01;//n;
	public static final int VALUE_LAZY = -0x02;
	public static final int VALUE_NEW_LIST = -0x03;//[;
	public static final int VALUE_NEW_MAP = -0x04;//{;
	
	//九：（最高级别的运算符号）
	public static final int OP_GET_PROP = 0<<5 | 8<<1 |1;//.;
	public static final int OP_STATIC_GET_PROP = 1<<5 | 8<<1 |0;//.#;解析时可以忽略改节点，表达式优化的时候处理
	public static final int OP_INVOKE_METHOD = 2<<5 | 8<<1 |1;//()

	//八：（次高级别）
	public static final int OP_NOT = 0<<5 | 7<<1 |0;//!;
	public static final int OP_POS = 1<<5 | 7<<1 |0;//+;//正数
	public static final int OP_NEG = 2<<5 | 7<<1 |0;//-;//负数
	
	//七：
	public static final int OP_MUL = 0<<5 | 6<<1 |1;//*;
	public static final int OP_DIV = 1<<5 | 6<<1 |1;///;
	public static final int OP_MOD = 2<<5 | 6<<1 |1;//%;
	
	//六：
	//与正负符号共享了字面值
	public static final int OP_ADD = 0<<5 | 5<<1 |1;//+;//6
	//五：
	public static final int OP_SUB = 1<<5 | 4<<1 |1;//-;
	

	//四:
	public static final int OP_LT =    0<<5 | 3<<1 |1;//<;
	public static final int OP_GT =    1<<5 | 3<<1 |1;//>;
	public static final int OP_LTEQ =  2<<5 | 3<<1 |1;//<=;
	public static final int OP_GTEQ =  3<<5 | 3<<1 |1;//>=;
	public static final int OP_EQ =    4<<5 | 3<<1 |1;//==;
	public static final int OP_NOTEQ = 5<<5 | 3<<1 |1;//!=;

	//三：
	public static final int OP_AND = 0<<5 | 2<<1 |1;//&&;
	public static final int OP_OR =  1<<5 | 2<<1 |1;//||;

	//二：
	public static final int OP_QUESTION =        0<<5 | 1<<1 |1;//?;
	public static final int OP_QUESTION_SELECT = 1<<5 | 1<<1 |1;//:;

	//一：
	//与Map Join 共享字面量（map join 会忽略）
	public static final int OP_PARAM_JOIN = 0<<5 | 0<<1 |1;//,
	//与三元运算符共享字面值
	public static final int OP_MAP_PUSH = 1<<5 | 0<<1 |1;//:,
	

	
	
	public abstract int getType();

	public abstract String toString();

	public abstract Object getParam();
}
