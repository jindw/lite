package org.xidea.el;


/**
 * 运算符编号说明：
 * 0<<12       |   0<<8  |  0<<6  | 0<<2 |  0
 * 1111            1111      11     1111   11      
 * 二级组内序号   二级优先级     参数标志   优先级  组内序号
 * 
 * 参数标志 说明：
 * 00 一位操作符
 * 01 两位操作符
 * 10 保留标志(三位操作符?)
 * 11 保留标志(四位操作符?)
 * @author jindw
 */
public abstract interface ExpressionToken {
	public static final int BIT_PRIORITY     = 15<<2;
	public static final int BIT_PRIORITY_SUB = 15<<8;
	public static final int BIT_ARGS         = 3<<6;
	public static final int POS_INC = 12;

	//值类型（<=0）
	//常量标记（String,Number,Boolean,Null）
	public static final int VALUE_CONSTANTS = -0x01;//value
	public static final int VALUE_VAR       = -0x02;//var
	public static final int VALUE_LIST      = -0x03;//[]
	public static final int VALUE_MAP       = -0x04;//{}
	public static final int VALUE_LAMBDA    = -0x05;//{}
	
	
	//九：（最高级别的运算符号） OP_GET,.[]
	public static final int OP_GET      = 0<<12 | 0<<8 | 1<<6 | 8<<2 | 0;//.
	public static final int OP_INVOKE   = 0<<12 | 0<<8 | 1<<6 | 8<<2 | 1;//()
	
	//八
	public static final int OP_NOT     = 0<<12 | 0<<8 | 0<<6 | 7<<2 | 0;//!
	public static final int OP_BIT_NOT = 0<<12 | 0<<8 | 0<<6 | 7<<2 | 1;//~
	public static final int OP_POS     = 0<<12 | 0<<8 | 0<<6 | 7<<2 | 2;//+
	public static final int OP_NEG     = 0<<12 | 0<<8 | 0<<6 | 7<<2 | 3;//-
	
	//七：
	public static final int OP_MUL = 0<<12 | 0<<8 | 1<<6 | 6<<2 | 0;//*
	public static final int OP_DIV = 0<<12 | 0<<8 | 1<<6 | 6<<2 | 1;///
	public static final int OP_MOD = 0<<12 | 0<<8 | 1<<6 | 6<<2 | 2;//%
	
	//六：
	//与正负符号共享了字面值
	public static final int OP_ADD = 0<<12 | 0<<8 | 1<<6 | 5<<2 | 0;//+
	public static final int OP_SUB = 0<<12 | 0<<8 | 1<<6 | 5<<2 | 1;//-
	
	//五:移位
	public static final int OP_LSH   =  0<<12 | 0<<8 | 1<<6 | 4<<2 | 0;//<<
	public static final int OP_RSH   =  0<<12 | 0<<8 | 1<<6 | 4<<2 | 1;//>>
	public static final int OP_URSH   =  0<<12 | 0<<8 | 1<<6 | 4<<2 | 2;//>>>
	
	//四:比较
	public static final int OP_LT   =  0<<12 | 1<<8 | 1<<6 | 3<<2 | 0;//<
	public static final int OP_GT   =  0<<12 | 1<<8 | 1<<6 | 3<<2 | 1;//>
	public static final int OP_LTEQ =  0<<12 | 1<<8 | 1<<6 | 3<<2 | 2;//<=
	public static final int OP_GTEQ =  0<<12 | 1<<8 | 1<<6 | 3<<2 | 3;//>=
	public static final int OP_IN   =  1<<12 | 1<<8 | 1<<6 | 3<<2 | 0;// in 
	
	//四:等不等比较
	public static final int OP_EQ        =  0<<12 | 0<<8 | 1<<6 | 3<<2 | 0;//==
	public static final int OP_NE        =  0<<12 | 0<<8 | 1<<6 | 3<<2 | 1;//!=
	public static final int OP_EQ_STRICT =  0<<12 | 0<<8 | 1<<6 | 3<<2 | 2;//===
	public static final int OP_NE_STRICT =  0<<12 | 0<<8 | 1<<6 | 3<<2 | 3;//!==
	
	//三:按位与或
	public static final int OP_BIT_AND = 0<<12 | 4<<8 | 1<<6 | 2<<2 | 0;//&
	public static final int OP_BIT_XOR = 0<<12 | 3<<8 | 1<<6 | 2<<2 | 0;//^
	public static final int OP_BIT_OR  = 0<<12 | 2<<8 | 1<<6 | 2<<2 | 0;//|
	//三:与或
	public static final int OP_AND = 0<<12 | 1<<8 | 1<<6 | 2<<2 | 0;//&&
	public static final int OP_OR  = 0<<12 | 0<<8 | 1<<6 | 2<<2 | 0;//||

	//二：
	//?;
	public static final int OP_QUESTION        = 0<<12 | 0<<8 | 1<<6 | 1<<2 | 0;//?
	//:;
	public static final int OP_QUESTION_SELECT = 0<<12 | 0<<8 | 1<<6 | 1<<2 | 1;//:
	
	//一：
	//与Map Join 共享字面量（map join 会忽略）
	public static final int OP_JOIN   = 0<<12 | 0<<8 | 1<<6 | 0<<2 | 0;//,
	//与三元运算符共享字面值
	public static final int OP_PUT   = 0<<12 | 0<<8 | 1<<6 | 0<<2 | 1;//:
	

	
	
	public int getType();
	public String toString();
	public ExpressionToken getLeft();
	public ExpressionToken getRight();
	public Object getParam();
}
