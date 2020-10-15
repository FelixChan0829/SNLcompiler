package mycompiler.jieshiqi;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.applet.*;
/* 指令结构类型:操作码,操作数1,操作数2,操作数3 */
class Instruction
{
    String iop;
    int iarg1;
    int iarg2;
    int iarg3;
} 
/*输入窗口*/
class Mywindow extends Frame
{
    Button button1;
    Label label1,label2;
    TextField text1;
    String stext;

    Mywindow(String s)
    {
	super(s);

	Panel panel1=new Panel(),panel2=new Panel(),panel3=new Panel();
        button1=new Button("确定");
        label1=new Label(s);
        label2=new Label("TM  simulation (enter h for help)...");
        text1=new TextField(10);

	panel1.add(label2);
        panel2.add(label1);
        panel2.add(text1);
	panel3.add(new Label());
	panel3.add(button1);
	panel3.add(new Label());

        setLayout(new GridLayout(3,1));
        add(panel1);
        add(panel2);
        add(panel3);
        setSize(60,70);
        setVisible(false);
        pack();
	
        addWindowListener(new WindowAdapter() 
        {
            public void windowClosing(WindowEvent e)
            {setVisible(false); System.exit(0);}  
        });
    }
}
/*显示窗口*/
class XWindow extends Frame 
{
    TextArea text;
    XWindow(String name,String s)
    {
	super(name);
	setLayout(new GridLayout(1,1));
        text=new TextArea("",50,100);
        text.setText(s);
        add(text);
        setSize(50,100);
	setVisible(false);
	pack();

        addWindowListener(new WindowAdapter() 
        {
            public void windowClosing(WindowEvent e)
            {setVisible(false); System.exit(0);}  
        });
    }
}

/*****************************************************/
/* 类  名 Translator	                             */
/* 功  能 总程序的处理				     */
/* 说  明 建立一个类，处理总程序                     */
/*****************************************************/
public class Translator extends Applet implements ActionListener
{
/***************** 常量 *******************/

/* 为大型程序扩展,指令存储区大小,定义为1024 */
int IADDR_SIZE = 1024;
/* 为大型程序扩展,数据存储区大小,定义为1024 */
int DADDR_SIZE = 1024; 
/* 寄存器数量,定义为8 */
int NO_REGS = 8;
/* PC寄存器,定义为7 */
int PC_REG = 7;
/* 目标代码行大小,定义为121 */
int LINESIZE = 121;
/* 字大小,定义为20 */
int WORDSIZE = 20;

/******** 变量 ********/
int iloc = 0;			/* 指令存储计数指针,初始为0 */
int dloc = 0;			/* 数据存储计数指针,初始为0 */
boolean traceflag = false;	/* 指令执行追踪标志,初始为FALSE */
boolean icountflag = false;	/* 指令执行计数标志,初始为FALSE */

/* iMem用于指令存储,为1024长的指令结构数组 */
Instruction iMem[]=new Instruction[IADDR_SIZE];				

/* dMem用于数据存储,为1024长的整数类型数组 */
int dMem[]=new int[DADDR_SIZE];						

/* reg用于寄存器存储,为8长的整数类型数组 */
int reg[]=new int[NO_REGS];							

/* 指令操作码表,对应寻址模式分为三类,共20个字符串*/
String opCodeTab[ ] = 
{"HALT","IN","OUT","ADD","SUB","MUL","DIV","????",
"LD","ST","????", 
"LDA","LDC","JLT","JLE","JGT","JGE","JEQ","JNE","????"
};
int opRR=7;    /*第一个"????"的位置,它的前面为寄存器寻址模式指令类型*/
int opRM=10;   /*第二个"????"的位置,它的前面为寄存器-内存寻址模式指令类型*/
int opRA=19;   /*第三个"????"的位置,它的前面为寄存器-立即数寻址模式指令类型*/

/** 单步执行结果状态表 **/
//String stepResultTab[] = 
//{"OK","Halted","Instruction Memory Fault","Data Memory Fault","Division by 0"};

String pgm;                     /* 用于存储目标代码 */
char in_Line[]=new char[LINESIZE];  /* 用于存储一行代码 */
int lineLen;		        /* in_Line中代码的长度 */
int inCol;			/* 用于指出在in_Line中的当前字符位置 */
int num;			/* 用于存储当前所得数值 */
String word;	                /* 用于存储当前的字 */
char ch;			/* 当前代码行中当前位置上的字符 */
String name;                    /* 显示窗口的名字 */
String expr="\n";                    /* 显示窗口的内容 */
int in_s;                       /* 在函数actionPerformed与函数stepTM间传递一个int值 */
boolean do_com=true;            /* 输入命令是否为q(退出) */
String stepResult;              /* 结果状态 */
char cmd;		        /* 用户输入命令简称 */
int stepcnt=0;                  /* 执行命令数 */

Mywindow win1;        /* 输入命令窗口 */
Mywindow win2;   /* 输入值窗口 */
XWindow xwin;           /* 显示窗口 */

public boolean Error1=false;
public boolean Error=false;
public String yerror;
public String serror;  

public Translator(String s)
{     
    Target t = new Target(s);
    if (t.Error1)
    {
        Error1=true;
        serror=t.serror;
    }
    else if (t.Error)
    {
        Error=true;
        yerror=t.yerror;
    }
    else
        tmain(t.mbcode);
}

/********************************************/
/* 函数名 tmain			            */
/* 功  能 tm机主执行函数                    */
/* 说  明 函数完成tm机的命令处理,	    */
/*	  并解释执行目标指令	            */
/********************************************/ 
void tmain(String codefile)
{ 
    pgm = codefile;	

    /* 目标代码文件为空,输出错误信息 */
    if (pgm == null)
    { 
        xwin=new XWindow("ERROR","TargetCode file is null");
        xwin.setVisible(true);
        return;
    }								

    /* 读入指令:将指令存储区iMem清空并从指定的文件中写入指令序列 */
    if (!readInstructions())
        return;

    /* 交互执行,处理用户输入的TM命令,对已经输入到iMem中的指令进行操作 */
    enterCom();
}

/********************************************************/
/* 函数名 readInstructions				*/
/* 功  能 指令文件读入函数				*/
/* 说  明 将指令文件中的指令逐条读入到指令存储区iMem	*/
/********************************************************/
boolean readInstructions()
{ 
    int op;		        /* 当前指令操作码在opCodeTab[]中的位置 */
    int arg1=0,arg2=0,arg3=0;		/* 当前指令操作数 */
    int loc,regNo,lineNo;

    /* 将8个寄存器内容初始化为0 */
    for (regNo = 0;regNo < NO_REGS;regNo++)
        reg[regNo] = 0;						

    /* dMem为数据存储区,0地址单元dMem[0]的值赋为数据存储区高端地址1023	*
     * 此数值将在目标程序运行时由程序的先驱指令读入到mp寄存器中	*/
    dMem[0] = DADDR_SIZE - 1;				

    /* 将数据存储数区内除0地址单元外的各单元初始化为0 */
    for (loc = 1;loc < DADDR_SIZE;loc++)
        dMem[loc] = 0;

    /* 将指令存储区中各单元初始化为指令;HALT 0,0,0 */
    for (loc = 0 ; loc < IADDR_SIZE ; loc++)
    { 
        iMem[loc]=new Instruction();
        iMem[loc].iop = "HALT";
        iMem[loc].iarg1 = 0;
        iMem[loc].iarg2 = 0;
        iMem[loc].iarg3 = 0;
    }

    lineNo = 0;		/* lineNo用于记录当前代码指令行号 */

    /*以\n为分隔符,将目标代码分成若干行*/
    StringTokenizer LineCode=new StringTokenizer(pgm,"\n");
    while (LineCode.hasMoreTokens())				
    { 
	String lineTok=LineCode.nextToken();
        lineLen=lineTok.length();
        in_Line=lineTok.toCharArray();

        inCol = 0;		/* 当前代码行in_Line中当前字符位置inCol初始为0 */
        lineNo++;		/* 当前代码行行号加1 */

	/* 当前字符不是"*",即不是注释语句,应该是指令语句 */
        if((nonBlank()) && (in_Line[inCol] != '*'))
        {
	    /* 当前字符不是数字,报地址错,并给出行号lineNo */
	    if (!getNum())
                return error("Bad location",lineNo,-1);

	    /* 将所得数值赋给当前代码地址标号loc */
	    loc = num;

	    /* 代码地址标号loc超出指令存储区地址IADDR_SIZE,报错 */
            if (loc > IADDR_SIZE)
                return error("Location too large",lineNo,loc);

	    /* 代码地址标号loc后面缺少冒号,报缺少冒号错 */
            if (!skipCh(':'))
                return error("Missing colon", lineNo,loc);

	    /* 当前位置不是单词,报缺少指令操作码错 */
            if (!getWord())
                return error("Missing opcode",lineNo,loc);

	    /* 初始查表op,op指向操作码表表首,值为0 */
            op=0;

	    /* 查操作码表opCodeTab,比较当前字word中的字符
               表中共有20个字符串 */
            while ((op < opRA) && (!(word.equals(opCodeTab[op]))))
                op = op+1;

	    /* 当前单词word中指定的操作码不在操作码表opCodeTab中,报非法操作码错误 */
            if(!(word.equals(opCodeTab[op])))
                return error("Illegal opcode",lineNo,loc);

	    /* 对查表得到的操作码值op的寻址模式,进行分类处理 */
            String s_op=opClass(op);
            if(s_op.equals("opclRR"))
            { 			
                /* 寄存器寻址模式操作码 */
	        /* 第一寄存器操作数错,非0-7之间数字,	*
                 * 输出错误信息,行号lineNo,代码地址标号loc	*/
                if ((!getNum()) || (num < 0) || (num >= NO_REGS))
                    return error("Bad first register",lineNo,loc);

		/* 将第一操作数arg1赋值为当前数值num */
                arg1 = num;

		/* 第一操作数后漏掉","分隔符,报错 */
                if (!skipCh(','))
                    return error("Missing comma",lineNo,loc);

		/* 第二寄存器操作数错,非0-7之间数字,		*
		 * 输出错误信息,行号lineNo,代码地址标号loc	*/
                if ((!getNum()) || (num < 0) || (num >= NO_REGS))
                    return error("Bad second register",lineNo,loc);

		/* 将第二个操作数arg2赋值为当前数值num */
		arg2 = num;

		/* 第二操作数后漏掉","分隔符,报错 */
                if (!skipCh(',')) 
                    return error("Missing comma", lineNo,loc);

		/* 第三寄存器操作数错,非0-7之间数字,报错 */
                if ((!getNum()) || (num < 0) || (num >= NO_REGS))
                    return error("Bad third register",lineNo,loc);

		/* 将第三操作数arg3赋值为当前数值num */
                arg3 = num;
            }
            else if((s_op.equals("opclRM"))||(s_op.equals("opclRA")))
            {
		/* 寄存器-内存寻址模式		*
		 * 寄存器-立即数寻址模式	*/
		/* 第一寄存器操作数错,非0-7之间数字,报错 */
                if ((!getNum()) || (num < 0) || (num >= NO_REGS))
                    return error("Bad first register",lineNo,loc);

 		/* 将第一操作数arg1赋值为当前数值num */
		arg1 = num;

		/* 第一操作数后漏掉","分隔符,报错 */
                if (!skipCh(','))
                    return error("Missing comma",lineNo,loc);

		/* 第二偏移地址操作数错误,非数字偏移地址,报错 */
                if (!getNum())
                    return error("Bad displacement",lineNo,loc);

		/* 将第二偏移地址操作数arg2赋值为当前地址num */
                arg2 = num;

		/* 第二偏移地址操作数后漏掉"("或者是","分隔符,报错 */
                if ((!skipCh('(')) && (!skipCh(',')))
                    return error("Missing LParen or comma",lineNo,loc);

		/* 第二寄存器操作数错,非0-7之间数字,报错 */
		if ((!getNum()) || (num < 0) || (num >= NO_REGS))
                    return error("Bad second register",lineNo,loc);

		/* 将第三操作数arg3赋值为当前数值num */
                arg3 = num;
            }
	    /* 按代码地址标号loc将指令存储到指令存储区iMem */
            iMem[loc].iop = opCodeTab[op]; 
            iMem[loc].iarg1 = arg1;
            iMem[loc].iarg2 = arg2;
            iMem[loc].iarg3 = arg3;
        }
    }
    return true;
}  

/****************************************************/
/* 函数名 opClass				    */
/* 功  能 指令寻址模式分类函数			    */
/* 说  明 该函数对给定的指令操作码枚举值c进行分类   */
/*        返回指令所属寻址模式			    */
/****************************************************/
String opClass(int c)
{ 
    /* 如果枚举值c小于opRRLim(7),则指令为寄存器寻址模式指令类型 */
    if(c <= opRR) 
        return "opclRR";

    /* 如果枚举值c小于opRMLim(10),则指令为寄存器-内存寻址模式指令类型 */
    else if(c <= opRM) 
        return "opclRM";

    /* 为寄存器-立即数寻址模式指令类型 */
    else                    
       return "opclRA";
}
 
/****************************************************/
/* 函数名 opNum				            */
/* 功  能 指令寻址模式分类函数			    */
/* 说  明 该函数对给定的指令操作码枚举值c进行分类   */
/*        返回指令所属寻址模式			    */
/****************************************************/
int opNum(String s)
{
    if((s.equals("HALT"))||(s.equals("IN"))||(s.equals("OUT"))||(s.equals("ADD"))||(s.equals("SUB"))||(s.equals("MUL"))||(s.equals("DIV")))
        return 7;
    else if((s.equals("LD"))||(s.equals("ST")))
        return 10;
    else
        return 19;
}
/********************************************************/
/* 函数名 nonBlank				        */
/* 功  能 非空字符获取函数				*/
/* 说  明 如果成功从当前行中取得非空字符,函数返回TRUE	*/
/*	  否则,函数返回FALSE			        */
/********************************************************/
boolean nonBlank()
{ 
    /* 在当前代码行in_Line中,当前字符位置inCol中为空格字符	*   
     * 在当前代码行in_Line中,当前字符位置inCol下移,略过空格	*/
    while ((inCol < lineLen) && (in_Line[inCol] == ' ') )
        inCol++;

    /* 在当前代码行in_Line中,遇到非空字符 */
    if (inCol < lineLen)
    { 
        /* 取当前字符位置inCol中的字符送入ch,		*
	 * 函数返回TRUE(已定义为1),ch中得到非空字符	*/
	ch = in_Line[inCol];
        return true; 
    }
    /* 当前代码行已经读完,将当前字符ch 赋为空格,	*
     * 函数返回FALSE(已定义为0),ch中为空格字符	*/
    else
    { 
        ch = ' ';
        return false; 
    }
} 

/****************************************************/
/* 函数名 getCh					    */
/* 功  能 字符获取函数				    */
/* 说  明 如果当前行中字符未读完,则函数返回当前字符 */
/*	  否则,函数返回空格字符			    */
/****************************************************/
void getCh()
{ 
    /* 在当前代码行in_Line中,当前字符列数inCol未超过代码行实际长度lineLen *
     * 取得当前行中当前位置的字符,送入ch		*/
    if (++inCol < lineLen)
        ch = in_Line[inCol];

    /* 如果inCol超出当前代码行长度范围,则ch赋为空格 */
    else ch = ' ';
} 

/****************************************************************/
/* 函数名 getNum						*/
/* 功  能 数值获取函数						*/
/* 说  明 将代码行中连续出现的有加减运算的数term合并计数,       */
/*        所的数值送入为num.如果成功得到数值,则函数返回TRUE;	*/
/*        否则,函数返回FALSE					*/
/****************************************************************/
boolean getNum()
{ 
    int sign;				/* 符号因子 */
    int term;				/* 用于记录当前录入的局部数值 */
    boolean temp = false;		/* 记录函数返回值,初始为假 */
    num = 0;				/* 用于记录所有加减运算后的最终数值结果 */

    do
    { 
        sign = 1;			/* 符号因子初始为1 */

        /* 调用函数nonBlank()略过当前位置的空格后,			*
         * 所得到的当前非空字符ch为+或-.(+/-的连续出现处理)	*/
        while (nonBlank() && ((ch == '+') || (ch == '-')))
        { 
            temp = false;

	    /* 当前字符ch为"-"时,符号因子sign设为-1 */
	    if(ch == '-')  
                sign = - sign;

	    /* 取当前代码行中下一字符到当前字符ch中 */
            getCh();
        }
        term = 0;		/* 当前录入的局部数值初始为0 */
        nonBlank();		/* 略过当前位置上的空格 */

	/* 当前字符ch为数字,局部数值的循环处理 */
        while (isdigit(ch))				
        { 
            temp = true;		/* 函数返回值设为TRUE,成功得到数字 */

	    /* 将字符序列转化为数值形式,进行进位累加 */
            term = term * 10 + ( (int)ch - (int)('0') );

            getCh();			/* 取当前代码行中下一字符到当前字符ch中 */

        }
	/* 将局部数值带符号累加,得到最终数值num */
        num = num + (term * sign);
    } while ((nonBlank()) && ((ch == '+') || (ch == '-')));
    return temp;
}

/****************************************************/
/* 函数名  isdigit				    */
/* 功  能  检查参数c是不是数字			    */
/* 说  明  					    */
/****************************************************/
boolean isdigit(char c)
{
    if ((c=='0')||(c=='1')||(c=='2')||(c=='3')||(c=='4')||(c=='5')||(c=='6')||        (c=='7')||(c=='8')||(c=='9'))
        return true;
    else return false;
}

/****************************************************/
/* 函数名 getWord				    */
/* 功  能 单词获取函数				    */
/* 说  明 函数从当前代码行中获取单词.如果得到字符,  */
/*	  则函数返回TRUE;否则,函数返回FALSE	    */
/****************************************************/
boolean getWord()
{ 	
    boolean temp = false;			/* 函数返回值初始为FALSE */
    int length = 0;			/* 单词长度初始为0 */
    char gword[]=new char[20];

    /* 在当前代码行中成功获取非空字符ch */
    if(nonBlank())
    {
        /* 当前非空字符ch为字母或数字 */
	while(isalpha(ch))
        {
            /* 当前单词word未超过规定字长WORDSIZE-1(为单词结束字符留一空位)	*
	     * 将当前字符ch读入到单词末尾		*/
	    if (length < WORDSIZE-1)
                gword[length++] = ch;

            getCh();			/* 取当前代码行中下一字符 */
        }
	/* 给当前单词word加入结束字符 */
        word=new String(gword);
        word=word.trim();

	/* 设置函数返回值,当读入字word非空的时候为TRUE */
        temp = (length != 0);
    }
    return temp;
} 

/****************************************************/
/* 函数名  isalpha				    */
/* 功  能  检查参数c是不是字母			    */
/* 说  明  					    */
/****************************************************/
boolean isalpha(char c)
{
    if ((c=='a')||(c=='b')||(c=='c')||(c=='d')||(c=='e')||(c=='f')||(c=='g')||        (c=='h')||(c=='i')||(c=='j')||(c=='k')||(c=='l')||(c=='m')||(c=='n')||(c=='o')||(c=='p')||(c=='q')||(c=='r')||(c=='s')||(c=='t')||(c=='u')||(c=='v')||(c=='w')||(c=='x')||(c=='y')||(c=='z'))
        return true;
    else if ((c=='A')||(c=='B')||(c=='C')||(c=='D')||(c=='E')||(c=='F')||(c=='G')||(c=='H')||(c=='I')||(c=='J')||(c=='K')||(c=='L')||(c=='M')||(c=='N')||(c=='O')||(c=='P')||(c=='Q')||(c=='R')||(c=='S')||(c=='T')||(c=='U')||(c=='V')||(c=='W')||(c=='X')||(c=='Y')||(c=='Z'))
         return true; 
    else return false;
}

/************************************************************/
/* 函数名 skipCh					    */
/* 功  能 字符空过函数					    */
/* 说  明 如果当前位置上字符为函数指定的字符,则空过该字符,  */
/*        函数返回TRUE;否则函数返回FALSE	            */
/************************************************************/
boolean skipCh(char c)
{ 
    boolean temp = false;

    /* 当前位置上字符为函数指定字符c */
    if(nonBlank() && (ch == c))
    { 
        getCh();        /* 空过当前字符c,取下一字符 */
        temp = true;	/* 空过指定字符c,函数返回TRUE */
    }
    return temp;
} 

/************************************/
/* 函数名 atEOL			    */
/* 功  能 行结束判断函数	    */
/* 说  明 当前行是否结束的判断函数  */
/************************************/	
boolean atEOL()
{ 
    return (!nonBlank());	/* 如果当前行中没有非空字符,则函数返回TRUE */
} 

/****************************************************/
/* 函数名 error					    */
/* 功  能 错误处理函数				    */
/* 说  明 函数输出错误行号,指令地址标号和错误信息   */
/****************************************************/
boolean error(String msg,int lineNo,int instNo)
{ 
    String s;
    s="Line "+String.valueOf(lineNo);

    /* 输出错误指令地址标号instNo */
    if (instNo >= 0) 
        s=s+" (Instruction "+String.valueOf(instNo)+")";

    /* 输出错误信息msg */
    s=s+"   "+msg+"\n";

    xwin=new XWindow("ERROR",s);
    xwin.setVisible(true);

    return false;
}

/****************************************************/
/* 函数名 enterCom				    */
/* 功  能 读入指令函数				    */
/* 说  明 交互执行,处理用户输入的TM命令             */
/****************************************************/
void enterCom()
{
    win1=new Mywindow("Enter command: ");
    /* 屏幕显示提示信息,提示用户输入TM命令 */
    win1.setVisible(true);
    (win1.button1).addActionListener(this);
}

/****************************************************/
/* 函数名 enterData			            */
/* 功  能 读入指令函数				    */
/* 说  明 交互执行,处理用户输入的数值               */
/****************************************************/
void enterData()
{
    win2=new Mywindow("Enter value for IN instruction: ");
    /* 屏幕显示提示信息,提示用户输入TM命令 */
    win2.setVisible(true);
    (win2.button1).addActionListener(this);
}

/****************************************************/
/* 函数名 actionPerformed			    */
/* 功  能 处理事件接口函数		            */
/* 说  明 处理事件接口   		            */
/****************************************************/
public void actionPerformed(ActionEvent e)
{
    if(e.getSource()==win1.button1)   /*输入命令窗口*/
    {
        win1.stext=win1.text1.getText();
        lineLen = win1.stext.length();
        in_Line = win1.stext.toCharArray();
        inCol = 0;
        if(getWord())
        {
	    win1.setVisible(false);
            doCom(); 
        }  
    }
    else if(e.getSource()==win2.button1)  /*输入数值窗口*/
    {
        win2.stext=win2.text1.getText();
        lineLen = win2.stext.length();
        in_Line = win2.stext.toCharArray();
        inCol = 0;
        if(getNum())
        {
            reg[in_s] = num;
            win2.setVisible(false);
            if(cmd!='g')   /*决定进入哪个循环*/
            {
        	/* stepcnt此时用于记录将要执行,输出的指令或数据的数量,自减 */
                stepcnt--;
                if((stepcnt > 0) && (stepResult.equals("OKAY")))
                {
    		    /* 取得程序计数器reg[PC_REG]中当前指令地址 */
    		    iloc = reg[PC_REG];

    		    /* 根据执行指令追踪标志traceflag,将当前指令地址iloc上指令输出到屏幕 */
    		    if(traceflag) 
    		    {
        		name=" ";
        		writeInstruction(iloc);
    		    }
                    stepTM();
                }
                else
                    step0();
            }
    	    else
    	    {		
    		/* 执行过指令计数stepcnt加1 */
    		stepcnt++;

    		if(stepResult.equals("OKAY"))
    		{
           	    /* 根据执行指令追踪标志traceflag,将当前地址iloc上指令输出到屏幕 */
    		    iloc = reg[PC_REG];

    		    /* 根据执行指令追踪标志traceflag,将当前指令地址iloc上指令输出到屏幕 */
    		    if(traceflag) 
    		    {
        		name=" ";
        		writeInstruction(iloc);
    		    }
                    stepTM();
   	        }
    		else
    		{
		    /* 根据执行执行数量追踪标志icountflag,显示已经执行过的指令数量 */
        	    if (icountflag)
		    {
            		name="已经执行过的指令数量";
            		expr=expr+"Number of instructions executed ="+String.valueOf(stepcnt)+"\n";
        	    }
        	    step0();
    		}
    	    }
        }
        else    /*不是数值,报错*/
        {
            XWindow xwin2=new XWindow(null,"Illegal value\n");
            xwin2.setVisible(true);
        }         
    }
}

/****************************************************/
/* 函数名 doCom					    */
/* 功  能 处理命令函数				    */
/* 说  明 处理用户输入的命令                        */
/****************************************************/
void doCom()
{
    if(do_com)     /* 输入的命令不是q,继续执行,否则关闭窗口退出 */
       doCommand();  
    else
    {
        win1.setVisible(false);
        /* 虚拟机命令执行完毕 */
        xwin=new XWindow("OVER","Simulation done.");
        xwin.setVisible(true);
    }
}
/****************************************************/
/* 函数名 doCommand				    */
/* 功  能 TM机交互命令处理函数			    */
/* 说  明 函数处理用户输入的TM操作命令,完成相应动作 */
/****************************************************/
void doCommand()
{ 
    int i;
    int printcnt;
    int regNo,loc;
    name=null;

    do_com=true;
    cmd = word.charAt(0);  /* 取输入命令名中的第一个字符给cmd */
    switch(cmd)
    { 
 	/* 该命令用于设置指令执行追踪标志,追踪指令执行 */
        case 't' :
        traceflag = !traceflag;		/* 取反设置追踪标志traceflag */

        /* 输出TM机t命令执行结果信息 */
        String trac;
        trac="Tracing now ";
        if(traceflag) 
            trac=trac+"on.\n"; 
        else 
            trac=trac+"off.\n";

        name="追踪标志";
        expr=trac;

        break;
        /**************************************************************/

        /* 该命令输出帮助信息列表,显示各种命令及其功能 */
        case 'h' :

        String hel="Commands are:";

	/* 按步执行(step)命令:可输入"s(tep <n>"来执行,  *
	 * 可执行n(默认为1)条tm指令.			*/
        hel=hel+"   s(tep <n>	:Execute n (default 1) TM instructions\n";

	/* 执行到结束(go)命令:可输入"g(o"来执行,*
	 * 顺序执行tm指令直到遇到HALT指令		*/
        hel=hel+"   g(o	:Execute TM instructions until HALT\n";

	/* 显示寄存器(regs)命令:可输入"r(egs"来执行,*
	 * 显示各寄存器的内容				*/
        hel=hel+"   r(egs	:Print the contents of the registers\n";

	/* 输出指令(iMem)命令:可输入"i(Mem <b<n>>"来执行,*
	 * 从地址b处输出n条指令				*/
        hel=hel+"   i(Mem <b <n>>	:Print n iMem locations starting at b\n";

	/* 输出数据(dMem)命令:可输入"d(Mem<b<n>>"来执行,*
	 * 从地址b处输出n跳数据				*/
        hel=hel+"   d(Mem <b <n>>	:Print n dMem locations starting at b\n";

	/* 跟踪(trace)命令:可输入"t(race"来执行,	*
	 * 反置追踪标志traceflag,如果traceflag为TRUE,	*
	 * 则执行每条指令时候显示指令			*/
        hel=hel+"   t(race	:Toggle instruction trace\n";

	  /* 显示执行指令数量(print)命令:可输入"p(rint)"来执行,	*
	   * 反置追踪标志icountflag,如果icountflag为TRUE,	*
	   * 则显示已经执行过的指令数量.只在执行"go"命令时有效	*/
        hel=hel+"   p(rint	:Toggle print of total instructions executed"+"(go  only)"+"\n";

	  /* 重置tm机用(clear)命令:可输入"c(lear"来执行,	*
	   * 重新设置tm虚拟机,用以执行新的程序.			*/
        hel=hel+"   c(lear	:Reset simulator for new execution of program\n";

	  /* 帮助(help)命令:可输入"h(elp"来执行,显示命令列表 */
        hel=hel+"   h(elp	:Cause this list of commands to be printed\n";

	  /* 终止(quit)命令,可输入"q(uit"来执行,结束虚拟机的执行 */
        hel=hel+"   q(uit	:Terminate the simulation\n";

        name="显示命令及其功能";
        expr=hel;

        break;
        /**************************************************************/

	/* 跟踪显示所有执行过指令的p命令 */
        case 'p' :

        icountflag = !icountflag;		/* 设置执行指令计数标志 */

	/* 输出p命令执行的结果信息 */
        String pstr="Printing instruction count now ";
        if (icountflag) 
            pstr=pstr+"on.\n"; 
        else
            pstr=pstr+"off.\n";

        name="显示所有执行过指令";
        expr=pstr;

        break;
        /**************************************************************/

	/* 按步执行s命令 */
        case 's' :

	/* 缺省的命令模式,不带命令参数,单步执行 */
        if (atEOL())  
            stepcnt = 1;
	/* 带有命令参数的命令模式,取得参数stepcnt,取绝对值 */
        else if (getNum()) 
        { 
            if(num>0)
                stepcnt = num;
            else 
                stepcnt = -num;
        }
	/* 输出未知命令执行步数信息 */
        else 
        { 
            name="未知命令执行步";
            expr="Step count?\n";
	}
        break;
        /**************************************************************/


	/* 执行到结束g命令 */
        case 'g' :   
         
        stepcnt = 1;    
        break;
        /**************************************************************/

        /* 显示寄存器内容(regs)命令 */
        case 'r' :

  	/* 格式化显示所有寄存器内容 */
        String rstr="\n";
        for (i = 0;i < NO_REGS;i++)
            rstr=rstr+String.valueOf(i)+":"+String.valueOf(reg[i])+"\n";

        name="显示寄存器内容";
        expr=rstr;
        break;
        /**************************************************************/

	/* 输出指令存储区iMem中指令的i命令 */
        case 'i' :

	/* 初始化输出指令数printcnt为1 */
	printcnt = 1;

        if(getNum())
        { 
	    /* 得到命令的第一个执行参数,iloc指定输出指令的开始地址 */
	    iloc = num;
		
	    /* 得到命令的第二个执行参数,printcnt指定输出指令的数量 */
            if (getNum()) 
                printcnt = num;
             
	    /* 指令地址iloc在指令存储区iMem地址范围中,			*
             * 且指令输出数量printcnt大于0,从iloc指定地址输出指定数量指令*/
            if((iloc >= 0) && (iloc < IADDR_SIZE) && (printcnt > 0))
                name="指令存储区中指令";
	    while((iloc >= 0) && (iloc < IADDR_SIZE) && (printcnt > 0))
            { 
                writeInstruction(iloc);
                iloc++;
                printcnt--;
            }
        }
	/* 未给定指令开始地址和输出指令数量 */
        else
        {
            name="未给定指令开始地址和输出指令数量";
            expr="Instruction locations?\n";
        }
        break;
        /**************************************************************/

	/* 输出数据存储区dMem中的数据的d命令 */
        case 'd' :

	printcnt = 1;
        if(getNum())
        { 
	    /* 取得命令的第一执行参数,数据存储的开始地址dloc */
	    dloc = num;

		/* 取得命令的第二执行参数,输出数据的数量printcnt */
            if(getNum()) 
                printcnt = num;
   
            String dstr="\n";
  	    /* 给定数据地址dloc在数据存储区dMen地址范围内,		*
	     * 且数据输出数量printcnt大于0,从dloc指定地址输出指定数量的数据 */
	    while((dloc >= 0) && (dloc < DADDR_SIZE) && (printcnt > 0))
            { 
                dstr=dstr+String.valueOf(dloc)+"  "+String.valueOf(dMem[dloc])+"\n";
                dloc++;
                printcnt--;
            }
            name="未给定数据存储区中的数据开始地址和数量";
            expr=dstr;
        }
	/* 未给定数据存储区中的数据开始地址和数量 */
        else
	{
            name="未给定数据存储区中的数据开始地址和数量";
            expr="Data locations?\n";
	}
        break;
        /**************************************************************/

        /* 重置tm机用以执行新的程序(clear)指令 */
        case 'c' :

        iloc = 0;		/* 指令存储地址,初始为0 */
        dloc = 0;		/* 数据存储地址,初始为0 */
        stepcnt = 0;		/* 指令执行步数,初始为0 */

	/* 初始化各寄存器reg[]为0 */
        for(regNo = 0;regNo < NO_REGS;regNo++)
            reg[regNo] = 0;			

	/* 数据存储区0地址单元用于记录数据存储区dMem的高端地址 */
        dMem[0] = DADDR_SIZE - 1;

	/* 初始化其它数据存储区单元为0 */
        for(loc = 1;loc < DADDR_SIZE;loc++)
            dMem[loc] = 0;				
        break;
        /**************************************************************/

        case 'q' : 

        do_com=false;		/* 停止执行并退出命令 */
        return;
        /**************************************************************/

	/* 其它未定义命令,输出错误信息 */
        default : 
        {
            name="未定义命令";
            expr="Command "+cmd+" unknown.\n";
        }
        break;
    }  /* case */

    /******************** 命令的后续处理 **********************/
    stepResult = "OKAY";		/* 命令执行结果为OKAY */

    if (stepcnt > 0)
    {
        if (cmd == 'g')
        { 
            /* 此处stepcnt作为已经执行过的指令数目 */
            stepcnt = 0;
            /* 根据执行指令追踪标志traceflag,将当前地址iloc上指令输出到屏幕 */
            iloc = reg[PC_REG];
            if(traceflag) 
            {
       		name=" ";
        	writeInstruction(iloc);
            }

            /* 单步执行当前指令,结果返回stepResult */
            stepTM();
        }
        else 
        {
   	    /* 取得程序计数器reg[PC_REG]中当前指令地址 */
    	    iloc = reg[PC_REG];

    	    /* 根据执行指令追踪标志traceflag,将当前指令地址iloc上指令输出到屏幕 */
    	    if(traceflag) 
    	    {
        	name=" ";
        	writeInstruction(iloc);
    	    }

    	    /* 执行当前指令,结果返回stepResult */
    	    stepTM();
        }
    } 
    else
        step0(); 
}

/************************************************/
/* 函数名 step0				        */
/* 功  能 			                */
/* 说  明 命令执行完毕,输出结果	                */
/************************************************/
void step0()
{
    /* 根据执行结果的枚举值,查执行结果状态表,显示结果状态 */
    if(name==null)
    {
        name="结果状态";
        expr=expr+"\n"+"结果状态"+String.valueOf(stepResult)+"\n";
    }
    else
       expr=expr+"\n"+"结果状态"+":"+String.valueOf(stepResult)+"\n";
    xwin=new XWindow(name,expr);
    xwin.setVisible(true);

    expr="\n";
    do_com=true;
    enterCom();
}

/************************************************/
/* 函数名 stepTM				*/
/* 功  能 TM机单步执行函数			*/
/* 说  明 函数为一条指令解释执行,完成指令动作.	*/
/************************************************/
void stepTM()
{ 
    /* currentinstruction 用于存储当前将执行的指令 */
    Instruction currentinstruction;		

    int pc;			/* 程序计数器 */
    int r=0,s=0,t=0,m=0;	/* 指令操作数 */  
    boolean ok;	
    String ssiop;		

    do
    {
        r=0;
        s=0;
        t=0;
        m=0;

        /* pc设置为第7个寄存器reg[7]的值,为程序计数器 */
        pc = reg[PC_REG];						

        if((pc < 0) || (pc > IADDR_SIZE))
        {
            /* pc的值不是指令存储区的有效地址,报指令存储错,函数返回IMEM_ERR */
            stepResult="IMEM_ERR";
            return;
        }

        /* pc的值为有效指令地址,将程序计数器reg[PC_REG]的值加1 */
        reg[PC_REG] = pc + 1;

        /* 从指令存储区iMem之中取出当前指令 */
        currentinstruction = iMem[pc];

        /* 对取出的指令的寻址模式分类处理,初始化各个指令操作数变量 */
        String siop=opClass(opNum(currentinstruction.iop));
        if(siop.equals("opclRR"))
        { 
            /* 寄存器寻址模式 */
            r = currentinstruction.iarg1;
            s = currentinstruction.iarg2;
            t = currentinstruction.iarg3;
        }
        else if(siop.equals("opclRM"))
        {		
            /* 寄存器-内存寻址模式 */
	    r = currentinstruction.iarg1;
            s = currentinstruction.iarg3;
            m = currentinstruction.iarg2 + reg[s];
      
	    /* 操作数m非数据存储区有效地址,报数据存储错,函数返回DMEM_ERR */
	    if((m < 0) || (m > DADDR_SIZE))
           {
                stepResult="DMEM_ERR";
                return;
           }
        }
        else if(siop.equals("opclRA"))
        {		
            /* 寄存器-立即数寻址模式 */
            r = currentinstruction.iarg1;
            s = currentinstruction.iarg3;
            m = currentinstruction.iarg2 + reg[s];
        }

        /* 对将执行指令的操作码值进行分类处理,输出指令信息,	*
         * 完成指令动作,返回相应结果状态		*/
        ssiop=currentinstruction.iop;
        /******************** RR指令 ******************/
        /**********************************************/
        /**********************************************/
        if(ssiop.equals("IN")) 
        {
             in_s=r;
             break;
        }
        else 
    	{
    	    if(ssiop.equals("HALT"))
    	    {
           	 /* 格式化屏幕显示HALT(停止)指令,返回状态HALT(停止) */
          	 expr=expr+"HALT: "+String.valueOf(r)+","+String.valueOf(s)+","+String.valueOf(t)+"\n";
        	 stepResult="HALT";
    	    }

    	    /**********************************************/
    	    else if(ssiop.equals("OUT")) 
       	    /* 屏幕显示OUT指令执行的结果信息 */ 
        	expr=expr+"OUT instruction prints: "+String.valueOf(reg[r])+"\n";

            /**********************************************/
    	    else if(ssiop.equals("ADD")) 
	 	/* 完成ADD指令操作 */
        	reg[r] = reg[s] + reg[t]; 

    	    /**********************************************/
            else if(ssiop.equals("SUB")) 
		/* 完成SUB指令操作 */
        	reg[r] = reg[s] - reg[t]; 

    	    /**********************************************/
   	    else if(ssiop.equals("MUL")) 
		/* 完成MUL指令操作 */
        	reg[r] = reg[s] * reg[t]; 

    	    /**********************************************/
    	    else if(ssiop.equals("DIV")) 
            {
		/* 对于除法指令,若除数为0,则报除零错误, *
	 	* 并返回ZERODIVIDE;否则,完成除法操作 */
		if(reg[t] != 0) 
            	    reg[r] = reg[s] / reg[t];
        	else 
           	    stepResult="ZERODIVIDE";
    	    }

   	    /***************** RM 指令 ********************/
    	    /**********************************************/
    	    else if(ssiop.equals("LD")) 
		/* 将数据存储区dMem中的数据载入到寄存器reg[r] */
       		 reg[r] = dMem[m]; 
 
    	    /**********************************************/
   	    else if(ssiop.equals("ST")) 
		/* 将寄存器reg[r]中的数据写入到数据存储区dMem */
        	dMem[m] = reg[r];  
		
    	    /***************** RA 指令 ********************/
    	    /**********************************************/
    	    else if(ssiop.equals("LDA")) 
		/* 将寄存器reg[r]赋值为操作数m的值 */
        	reg[r] = m; 

    	    /**********************************************/
    	    else if(ssiop.equals("LDC")) 
		/* 将寄存器reg[r]赋值为当前指令的第二操作数的值 */
        	reg[r] = currentinstruction.iarg2; 

            /**********************************************/
    	    else if(ssiop.equals("JLT")) 
    	    {
		/* 如果寄存器reg[r]的值小于0,则将程序计数器reg[PC_REG]的值	*
		 * 赋值为立即数m,产生小于条件跳转		*/
        	if(reg[r] <  0) 
            	    reg[PC_REG] = m; 
    	    }

    	    /**********************************************/
    	    else if(ssiop.equals("JLE")) 
    	    {
		/* 如果寄存器reg[r]的值小于等于0,则将程序计数器reg[PC_REG]的值	*
		 * 赋值为立即数m,产生小于等于条件跳转		*/
        	if(reg[r] <=  0) 
           	    reg[PC_REG] = m;
    	    }

    	    /**********************************************/
    	    else if(ssiop.equals("JGT")) 
    	    {
		/* 如果寄存器reg[r]的值大于0,则将程序计数器reg[PC_REG]的值	*
	 	* 赋值为立即数m,产生大于条件跳转		*/
        	if(reg[r] >  0) 
            	    reg[PC_REG] = m; 
    	    }

    	    /**********************************************/
    	    else if(ssiop.equals("JGE"))
    	    { 
		/* 如果寄存器reg[r]的值大于等于0,则将程序计数器reg[PC_REG]的值	*
	 	* 赋值为立即数m,产生大于等于跳转			*/
        	if(reg[r] >=  0) 
            	    reg[PC_REG] = m;
    	    }

    	    /**********************************************/
    	    else if(ssiop.equals("JEQ"))
    	    { 
		/* 如果寄存器reg[r]的值等于0,则将程序计数器reg[PC_REG]的值	*
	 	* 赋值为立即数m,产生等于条件跳转		*/
        	if (reg[r] == 0) 
            	    reg[PC_REG] = m; 
    	    }

    	    /**********************************************/
    	    else if(ssiop.equals("JNE")) 
    	    {
		/* 如果寄存器reg[r]的值不等于0,则将程序计数器reg[PC_REG]的值	*
	 	* 赋值为立即数m,产生不等于条件跳转			*/
        	if (reg[r] != 0) 
            	    reg[PC_REG] = m;
    	    }
    	    if(cmd!='g')   /*决定进入哪个循环*/
            {
        	/* stepcnt此时用于记录将要执行,输出的指令或数据的数量,自减 */
                stepcnt--;
                if((stepcnt > 0) && (stepResult.equals("OKAY")))
                {
    		    /* 取得程序计数器reg[PC_REG]中当前指令地址 */
    		    iloc = reg[PC_REG];

    		    /* 根据执行指令追踪标志traceflag,将当前指令地址iloc上指令输出到屏幕 */
    		    if(traceflag) 
    		    {
        		name=" ";
        		writeInstruction(iloc);
    		    }
                }
                else
                    step0();
            }
    	    else
    	    {		
    		/* 执行过指令计数stepcnt加1 */
    		stepcnt++;

    		if(stepResult.equals("OKAY"))
    		{
           	    /* 根据执行指令追踪标志traceflag,将当前地址iloc上指令输出到屏幕 */
    		    iloc = reg[PC_REG];

    		    /* 根据执行指令追踪标志traceflag,将当前指令地址iloc上指令输出到屏幕 */
    		    if(traceflag) 
    		    {
        		name=" ";
        		writeInstruction(iloc);
    		    }
   	        }
    		else
    		{
		    /* 根据执行执行数量追踪标志icountflag,显示已经执行过的指令数量 */
        	    if (icountflag)
		    {
            		name="已经执行过的指令数量";
            		expr=expr+"Number of instructions executed ="+String.valueOf(stepcnt)+"\n";
        	    }
        	    step0();
    		}
    	    }
        }
    }while(stepResult.equals("OKAY"));
    if(ssiop.equals("IN")) 
        enterData();
} 

/********************************************************/
/* 函数名 writeInstruction				*/
/* 功  能 指令输出函数					*/
/* 说  明 该函数将指令存储区中指令以指定格式输出到屏幕	*/
/********************************************************/
void writeInstruction(int loc)
{  
    String wstr;
    /* loc为所要输出的指令在指令存储区中地址,输出到屏幕 */
    wstr=String.valueOf(loc);

    /* 输出指令地址loc在0-1023有效的指令存储区地址范围之内 */
    if ((loc >= 0)&&(loc < IADDR_SIZE))
    { 
        /* 输出地址为loc上的指令操作码值iMem[loc].iop和第一操作数iMem[loc].iarg1 */
	wstr=wstr+iMem[loc].iop+"   "+String.valueOf(iMem[loc].iarg1);

	/* 根据指令的寻址模式分类处理 */
        String ss=opClass(opNum(iMem[loc].iop));
        if(ss.equals("opclRR"))
            /* 输出指令为寄存器寻址模式指令,以给定形式输出操作数2,操作数3 */
	    wstr=wstr+String.valueOf(iMem[loc].iarg2)+","+String.valueOf(iMem[loc].iarg3);	
        else if((ss.equals("opclRM"))||(ss.equals("opclRA")))
            /* 输出指令为寄存器-立即数寻址模式指令,和寄存器-内存寻址模式指令	*
	     * 以给定形式输出操作数2,操作数3		*/
            wstr=wstr+String.valueOf(iMem[loc].iarg2)+"("+String.valueOf(iMem[loc].iarg3)+")";	
     }
     /* 向屏幕输出换行符 */
     wstr=wstr+"\n";

    expr=expr+wstr;
    //xwin1=new XWindow(null,wstr);/
    //xwin1.setVisible(true);
} 

}


class TokenType
{ 
    int lineshow;
    String Lex;
    String Sem;
} 
class ChainNodeType  
{
    TokenType  Token=new TokenType();      //单词
    ChainNodeType nextToken=null;          //指向下一个单词的指针
}
/******************************************/
class SymbTable  /* 在语义分析时用到 */
{
    String idName;
    AttributeIR  attrIR=new AttributeIR();
    SymbTable next=null;
}
class AttributeIR
{
    TypeIR  idtype=new TypeIR();
    String kind;	
    Var var;
    Proc proc;
}
class Var
{
    String access;
    int level;
    int off;
    boolean isParam;
}
class Proc
{
    int level;
    ParamTable param;
    int mOff;
    int nOff;
    int procEntry;
    int codeEntry;
}
class ParamTable
{
    SymbTable entry=new SymbTable();
    ParamTable next=null;
}
class TypeIR
{
    int size;
    String kind;
    Array array=null;
    FieldChain body=null;
}
class Array
{
    TypeIR indexTy=new TypeIR();
    TypeIR elementTy=new TypeIR();
    int low;
    int up;
}
class FieldChain
{
    String id;
    int off;
    TypeIR unitType=new TypeIR();
    FieldChain next=null;
}
/*******************************************/
class TreeNode   /* 语法树结点的定义 */
{
    TreeNode child[]=new TreeNode[3];
    TreeNode sibling=null;
    int lineno;
    String nodekind;
    String kind;
    int idnum;
    String name[]=new String[10];
    SymbTable table[]=new SymbTable[10];
    Attr attr=new Attr();
}   
class Attr
{
    ArrayAttr arrayAttr=null;  /* 只用到其中一个，用到时再分配内存 */
    ProcAttr procAttr=null;
    ExpAttr expAttr=null;
    String type_name;
}
class ArrayAttr
{
    int low;
    int up;
    String childtype;
}
class ProcAttr
{
    String paramt;
}
class ExpAttr
{
    String op;
    int val;
    String varkind;
    String type;
}
/*源程序对应的中间代码序列表示*/
class CodeFile
{   
    CodeR codeR=new CodeR();
    CodeFile former=null;
    CodeFile next=null;
}
/*中间代码的结构*/
class CodeR
{    
    String codekind;
    ArgRecord arg1;  
    ArgRecord arg2;
    ArgRecord arg3;
}  
class ArgRecord  
{  
    String form;
    MidAttr midAttr=new MidAttr();  /*变量的ARG结构需要纪录的信息*/
}  
class MidAttr		
{  
    int value;  /*纪录整数值*/
    int label;  /*纪录标号的值*/
    Addr addr;
}
class Addr
{ 
    String name;    /*注：变量名字已经没用，这里保留只是为了显示结果清晰*/
    int dataLevel;
    int dataOff;
    String access;  /*类型AccessKind在前面定义*/
}
/*常量定值表，用于常表达式优化*/
class ConstDefT
{ 
    ArgRecord variable=new ArgRecord();   /*用变量的ARG结构表示变量*/
    int constValue;       /*定值*/
    ConstDefT former=null;
    ConstDefT next=null;
} 
/*值编码表ValuNum*/
class ValuNum
{
    ArgRecord arg=new ArgRecord();
    String access;
    CodeInfo codeInfo=new CodeInfo();
    /*指向下一个节点指针*/
    ValuNum next=null;
}
class CodeInfo
{
    int valueCode;   /*直接变量，存储值编码*/
    TwoCode twoCode=null;
}
class TwoCode
{
    int valuecode;
    int addrcode;         /*间接临时变量，存储值编码和地址码*/
}
/*中间代码对应的映象码结构*/
class MirrorCode
{  
    int op1;
    int op2;
    int result;
} 
/*可用表达式代码表UsableExpr*/
class UsableExpr
{ 
    CodeFile code=null;	   /*中间代码地址*/
    MirrorCode mirrorC=null;    /*映象码*/
    UsableExpr next=null;  /*指向下一个节点*/
} 
/*临时变量的等价表TempEqua*/
class TempEqua
{
    ArgRecord arg1=null; /*被替换的临时变量*/
    ArgRecord arg2=null; /*用于替换的临时变量*/
    TempEqua next=null;
} 
/*循环信息表*/
class LoopInfo
{
    int state;            /*循环状况，为0时表示本层循环不可外提*/
    CodeFile whileEntry;  /*指向循环入口中间代码*/
    int varDef;           /*指向本层循环的变量地址表起始处*/
    CodeFile whileEnd;    /*指向循环出口中间代码*/
}    
/*循环信息栈*/
class LoopStack
{ 
    LoopInfo loopInfo;
    LoopStack under=null;
} 
/*标号地址表*/
class LabelAddr
{
    int label;
    int destNum;
    LabelAddr next=null;
} 
/*处理回填地址要用到的数据结构*/
class BackAddr
{  
    int backLoc;
    BackAddr former=null;
}
/*****************************************************/
/********************************************************************/
/* 类  名 Target	                                            */
/* 功  能 总程序的处理					            */
/* 说  明 建立一个类，处理总程序                                    */
/********************************************************************/
class Target
{
BackAddr AddrTop;
LabelAddr labelAddrT;
int AddrEMPTY;

int tmpOffset = 0;           /*临时变量区的偏移*/

/* TM指令当前生成代码写入地址 */
int emitLoc = 0 ;

/* 用于在函数emitSkip,emitBackup,emitRestore	
   中作为当前最高生成代码写入地址,初始为0 */
int highEmitLoc = 0;

boolean TraceCode=true;    /* 代码生成追踪标志 */

int mainOff;

/* 程序指令指示器pc为7,指向当前指令存储位置	
   程序指示器将使用寄存器数组中的第8个寄存器	*/
int pc=7;	

/* 过程活动记录头地址指示器sp指向过程活动记录的头地址*/
int sp=6;

/* 过程活动记录尾地址指示器top指向过程活动记录的尾地址 */
int top=5; 

/* 过程活动记录sp到display表距离指示器displayOff */
int displayOff=4;

/* 存储指示器mp指向用于临时变量存储的数据存储器顶端 */
int mp=3;

int ac2=2;      /* 第三累加器 */
int ac1=1;      /* 第二累加器 */
int ac=0;       /* 第一累加器 */

public boolean Error1=false;
public boolean Error=false;
public String yerror;
public String serror;
public String mbcode=" ";

public Target(String s)
{
    Opt o=new Opt(s);
    mainOff=o.mainOff;
    if (o.Error1)
    {
        Error1=true;
        serror=o.serror;
    }
    else if (o.Error)
    {
        Error=true;
        yerror=o.yerror;
    }
    else
        codeGen(o.mid);
}

/************************************************/
/************* 代码生成器的基本函数 *************/
/************************************************/
/* 函数名 codeGen				*/ 
/* 功  能 目标代码生成主函数			*/		
/* 说  明 该函数通过扫描中间代码序列产生目标代码*/
/************************************************/
void codeGen(CodeFile midcode)
{ 
    String s="File: 目标代码";

    /* 生成代码文件说明注释,写入代码文件 */
    emitComment("SNL Compilation to TM Code");
    emitComment(s);
 
    /* 生成标准先驱指令 */
    emitComment("Standard prelude:");
   
    /* 写入单元设置指令,清空0地址单元中内容 */
    emitRM("ST",ac,0,ac,"clear location 0");
   
    /* 写入注释,先驱指令写完 */
    emitComment("End of standard prelude.");
   
    /*为主程序入口留一个跳转语句*/
    int savedLoc = emitSkip(1);

    /*循环处理各条中间代码，调用相应得函数产生相应得目标代码*/
    while  (midcode!=null)
    {
	if ((midcode.codeR.codekind.equals("ADD"))||(midcode.codeR.codekind.equals("SUB"))||(midcode.codeR.codekind.equals("MULT"))||(midcode.codeR.codekind.equals("DIV"))||(midcode.codeR.codekind.equals("LTC"))||(midcode.codeR.codekind.equals("EQC")))
	    /*运算处理,包括算术运算和关系运算*/
	    arithGen(midcode);
	else if (midcode.codeR.codekind.equals("AADD"))	
	    /*地址加运算*/
	    aaddGen(midcode);
	else if (midcode.codeR.codekind.equals("READC"))		
	    /*输入语句*/
	    readGen(midcode);		
	else if (midcode.codeR.codekind.equals("WRITEC"))
	    /*输出语句*/
	    writeGen(midcode);		
	else if (midcode.codeR.codekind.equals("RETURNC"))
	    /*返回语句*/
            returnGen(midcode);		
	else if (midcode.codeR.codekind.equals("ASSIG"))
	    /*赋值语句*/
	    assigGen(midcode);		
	else if ((midcode.codeR.codekind.equals("LABEL"))||(midcode.codeR.codekind.equals("WHILESTART"))||(midcode.codeR.codekind.equals("ENDWHILE")))
	    /*标号声明语句*/
	    labelGen(midcode);		
	else if (midcode.codeR.codekind.equals("JUMP"))
	    /*跳转语句*/
	    jumpGen(midcode,1);	
	else if (midcode.codeR.codekind.equals("JUMP0"))
	    /*条件跳转语句*/
	    jump0Gen(midcode);		
	else if (midcode.codeR.codekind.equals("VALACT"))
	    /*形实参结合语句：形参是值参*/
	    valactGen(midcode);
	else if (midcode.codeR.codekind.equals("VARACT"))		
	    /*形实参结合语句：形参是变参*/
	    varactGen(midcode);
	else if (midcode.codeR.codekind.equals("CALL"))		
	    /*过程调用语句*/
	    callGen(midcode);
	else if (midcode.codeR.codekind.equals("PENTRY"))		
	    /*过程入口声明*/
	    pentryGen(midcode);
	else if (midcode.codeR.codekind.equals("ENDPROC"))		
	    /*过程出口声明*/
	    endprocGen(midcode);
	else if (midcode.codeR.codekind.equals("MENTRY"))	
	    /*主程序入口处理*/
	    mentryGen(midcode,savedLoc);
	else 
            mbcode=mbcode+" midcode  bug.\n"; 
    midcode = midcode.next;
    }

    /*处理完主程序，退出AR*/
    emitComment("<- end of main ");
    /* 写入注释,标志文件执行的结束 */
    emitComment("End of execution.");
    /* 写入停止指令,结束程序执行 */
    emitRO("HALT",0,0,0,"");
}
/************************************************/
/* 函数名 arithGen			        */ 
/* 功  能 生成算术运算的目标代码		*/		
/* 说  明				        */
/************************************************/
void arithGen(CodeFile midcode)
{
    /* 如果代码生成追踪标志TraceCode为TRUE,写入注释,标注操作开始 */
    if (TraceCode) 	  
        emitComment("-> Op");

    /*生成左操作数的目标代码，值存在ac中*/
    operandGen(midcode.codeR.arg1);

    /* 暂存左操作数在ac2中 */
    emitRM("LDA",ac2,0,ac,"op: store  left ");  
  
    /*生成右操作数的目标代码，值存在ac中*/
    operandGen(midcode.codeR.arg2);

    /* 取出左操作数存在ac1*/
    emitRM("LDA",ac1,0,ac2,"op: load left");

    /*根据操作符，生成运算的目标代码，ac中为计算结果*/
    if (midcode.codeR.codekind.equals("ADD"))
        /*相加*/
	emitRO("ADD",ac,ac1,ac,"op +");
    else if (midcode.codeR.codekind.equals("SUB"))
	/*相减*/
	emitRO("SUB",ac,ac1,ac,"op -");	
    else if (midcode.codeR.codekind.equals("MULT"))
	/*相乘*/
	emitRO("MUL",ac,ac1,ac,"op *");	
    else if (midcode.codeR.codekind.equals("DIV"))
	/*相除*/
	emitRO("DIV",ac,ac1,ac,"op /");  
    else if (midcode.codeR.codekind.equals("LTC"))
    {
	/*小于*/
        /* 写入减指令,将(左-右)操作数相减,结果送累加器ac */
	emitRO("SUB",ac,ac1,ac,"op <");  
        /* 写入判断跳转指令,如果累加器ac的值小于0,则代码指令指示器跳过两条指令*/
	emitRM("JLT",ac,2,pc,"br if true");
        /* 写入载入常量指令,将累加器ac赋值为0 */
	emitRM("LDC",ac,0,0,"false case"); 
        /* 写入数值载入指令,代码指令指示器pc跳过下一条指令 */
	emitRM("LDA",pc,1,pc,"unconditional jmp") ;
        /* 写入载入常量指令,将累加器ac赋值为1 */
	emitRM("LDC",ac,1,0,"true case");
    }
    else if (midcode.codeR.codekind.equals("EQC"))
    {
	/*等于*/
	/* 写入减法指令,将左,右操作数相减,结果送累加器ac */
	emitRO("SUB",ac,ac1,ac,"op ==");
        /* 写入判断跳转指令,如果累加器ac等于0,代码指令指示器pc跳过两条指令*/
	emitRM("JEQ",ac,2,pc,"br if true");
        /* 写入载入常量指令,将累加器ac赋值为0 */
	emitRM("LDC",ac,0,0,"false case");
        /* 写入数值载入指令,代码指令指示器pc跳过一条指令 */
	emitRM("LDA",pc,1,pc,"unconditional jmp") ;
        /* 写入载入常量指令,将累加器ac赋值为1 */
	emitRM("LDC",ac,1,0,"true case");
    }

    /*后面要用ac，故保存ac*/
    emitRM("LDA",ac2,0,ac,"op: store  result ");  

    /*计算目的操作数的地址，存在ac中*/
    FindAddr(midcode.codeR.arg3);

    /*取出暂存的计算结果，存入ac1*/
    emitRM("LDA",ac1,0,ac2,"op: load result");

    /*计算结果存入目的操作数*/
    emitRM("ST",ac1,0,ac, "");

    /* 如果代码生成追踪标志TraceCode为TRUE,写入注释信息,标注操作结束 */
    if (TraceCode)  
        emitComment("<- Op"); 
}
/************************************************/
/* 函数名 operandGen				*/ 
/* 功  能 生成操作数的目标代码			*/		
/* 说  明 分操作数为常数或者变量两种情况处理	*/
/*        注意不能用ac2				*/
/************************************************/
void operandGen(ArgRecord arg)
{
    if (arg.form.equals("ValueForm"))
    { 
        /*操作数为常数*/
        /* 如果代码生成追踪标志TraceCode为TRUE,写入注释,常数部分开始 */
	if (TraceCode) 
            emitComment("-> Const");

	/* 生成载入常量指令,载入常量到累加器ac */
	emitRM("LDC",ac,arg.midAttr.value,0,"load const");
	  
	/* 如果代码生成追踪标志TraceCode为TRUE,写入注释,常数部分结束 */
	if (TraceCode)  
            emitComment("<- Const");
    }
    else if (arg.form.equals("LabelForm"))
    {
        /*分量为标号*/
	/* 如果代码生成追踪标志TraceCode为TRUE,写入注释,标号部分开始 */
	if (TraceCode) 
            emitComment("-> Label");

	/* 生成载入标号指令,载入标号值到累加器ac */
	emitRM("LDC",ac,arg.midAttr.label,0,"load label");
	  
	/* 如果代码生成追踪标志TraceCode为TRUE,写入注释,标号部分结束 */
	if (TraceCode)  
            emitComment("<- Label");
    } 
    else if (arg.form.equals("AddrForm"))
    {
	/*操作数为变量,有可能是临时变量*/
	/* 如果代码生成追踪标志TraceCode为TRUE,写入注释,标注标识符开始 */
	if (TraceCode) 
            emitComment("-> var");
	  
	FindAddr(arg);
	/*其中ac返回的是源变量或临时变量的绝对偏移*/
	  
	if(arg.midAttr.addr.access.equals("indir"))
	{   
	    /*取内容作为地址,再取内容*/
	    emitRM("LD",ac1,0,ac,"indir load id value");
	    emitRM("LD",ac,0,ac1,"");
	}
	else
	{   
            /*存的是值*/
	    /* 写入数值载入指令,载入变量标识符的值*/
	    emitRM("LD",ac,0,ac,"load id value");
	}

	/* 如果代码生成追踪标志TraceCode为TRUE,写入注释,标注标识符结束 */
	if (TraceCode)  
            emitComment("<- var");
    }
}
/************************************************/
/* 函数名 aaddGen				*/ 
/* 功  能 生成地址加操作的目标代码		*/		
/* 说  明					*/
/************************************************/
void aaddGen(CodeFile midcode)
{	
    /* 如果代码生成追踪标志TraceCode为TRUE,写入注释,aadd语句开始 */
    if (TraceCode)  
        emitComment("->address  add");
  
    if (midcode.codeR.arg1.midAttr.addr.access.equals("dir"))
    {   
        /*ac中的地址即为基地址*/
	/*计算变量的绝对偏移,ac中存为变量的绝对偏移*/
        FindAddr(midcode.codeR.arg1);
    }
    else
    {   
        /*ac中的地址存放的内容为基地址*/
	/*计算变量的绝对偏移,ac中存为变量的绝对偏移*/
        FindAddr(midcode.codeR.arg1);
	emitRM("LD",ac,0,ac,"");
    }
    /*基地址转存到ac2*/
    emitRM("LDA",ac2,0,ac,"op: store  baseaddr ");  

    /*求地址相加运算的偏移量,存在ac中*/
    operandGen(midcode.codeR.arg2);

    /*地址相加,结果在ac2中*/
    emitRO("ADD",ac2,ac2,ac,"op +");
    
    /*求目的变量的地址，存入ac*/
    FindAddr(midcode.codeR.arg3);

    /*地址相加结果写入目的变量*/
    emitRM("ST",ac2,0,ac,"");
}
/************************************************/
/* 函数名 readGen				*/ 
/* 功  能 生成读操作的目标代码			*/		
/* 说  明 根据变量是直接变量还是间接变量进行	*/
/*	  不同的处理				*/
/************************************************/
void readGen(CodeFile midcode)
{
    /*生成读指令，该指令完成读入外部数值到累加器ac2的动作*/
    emitRO("IN",ac2,0,0,"read integer value");

    /*计算变量的绝对偏移,ac中存为变量的绝对偏移*/
    FindAddr(midcode.codeR.arg1);
    
    if(midcode.codeR.arg1.midAttr.addr.access.equals("dir"))
    {	
        /*直接存*/
	/*最后生成存储指令*/
	emitRM("ST",ac2,0,ac," var read : store value");
    }
    else
    {
	/*以ac内容作为地址找变量单元,再存*/
	emitRM("LD",ac1,0,ac,"");
	emitRM("ST",ac2,0,ac1," indir var read : store value");
    }
}
/************************************************/
/* 函数名 writeGen				*/ 
/* 功  能 生成写操作的目标代码			*/		
/* 说  明 调用函数得到值，并产生输出代码	*/
/************************************************/
void writeGen(CodeFile midcode)
{
    /*调用函数，得到输出的值，存在ac中*/
    operandGen(midcode.codeR.arg1);
	
    /*生成写指令，该指令完成将累加器ac中的值输出的动作*/
    emitRO("OUT",ac,0,0,"write ac");
}
/************************************************/
/* 函数名 returnGen				*/ 
/* 功  能 生成返回语句的目标代码		*/		
/* 说  明 返回过程调用的下一条语句，注意return  */
/*	  语句只在过程中出现			*/
/************************************************/
void returnGen(CodeFile midcode)
{
    /*从过程里跳出，所做的工作与过程结束相同*/
    endprocGen(midcode);
}
/************************************************/
/* 函数名 assigGen				*/ 
/* 功  能 生成赋值语句的目标代码		*/		
/* 说  明					*/
/************************************************/
void assigGen(CodeFile midcode)
{
    /* 如果代码生成追踪标志TraceCode为TRUE,写入注释,assign语句开始 */
    if (TraceCode)  
        emitComment("->assign");
    
    /*赋值左部变量的地址，存在ac中*/
    FindAddr(midcode.codeR.arg2);
    /*转放在ac2中*/
    emitRM("LDA",ac2,0,ac,"op: store  addr ");  

    /*生成赋值右部的目标代码，值存在ac中*/
    operandGen(midcode.codeR.arg1);

    if(midcode.codeR.arg2.midAttr.addr.access.equals("dir"))
	/*赋值,ac2中为地址*/
	emitRM("ST",ac,0,ac2,"var assign : store value");
    else
    {
	/*从ac2中取出内容，作为地址*/
	emitRM("LD",ac1,0,ac2," indir var assign");
	emitRM("ST",ac,0,ac1," store value");
    }
		
    /* 如果代码生成追踪标志TraceCode为TRUE,写入注释,assign语句结束 */
    if (TraceCode)  
        emitComment("<- assign") ;
}
/************************************************/
/* 函数名 labelGen				*/ 
/* 功  能 处理标号的目标代码生成		*/		
/* 说  明 若表中没有此标号对应得项,填标号地址表 */
/*        若表中有此标号对应项，则回填目标代码	*/
/************************************************/
void labelGen(CodeFile midcode)
{
    /*取得标号值*/
    int label = midcode.codeR.arg1.midAttr.label;

    /*取得当前目标代码编号*/
    int currentLoc = emitSkip(0) ;
  
    /*查找标号地址表*/
    LabelAddr item = labelAddrT;
    LabelAddr last = item;
    while (item!=null)
    {	
        if (item.label == label)
	    break;
        last = item;
        item = item.next;
    }

    if (item==null)  /*表中没有此标号对应得项,填表 */
    { 
        /*新建一项*/
	LabelAddr newItem = new LabelAddr();
        newItem.label = label;
	newItem.destNum = currentLoc;
        /*加入标号地址表中*/
        if (labelAddrT == null)
	    labelAddrT = newItem;
	else 
            last.next = newItem;
    }
    else /*表中有此标号对应项，则回填目标代码*/ 
    {  
	/*退回到指令回填地址*/
	emitBackup(item.destNum);
	/*写入跳转到此标号所在目标代码位置的代码*/
	emitRM("LDC",pc,currentLoc,0,"jump to label");
	/*恢复当前目标代码*/
	emitRestore();
    }
}
/************************************************/
/* 函数名 jumpGen				*/ 
/* 功  能 生成跳转的目标代码			*/		
/* 说  明 参数i是为了复用此函数而设，根据i	*/
/*	  决定从中间代码中哪个分量取标号值	*/
/************************************************/
void jumpGen(CodeFile midcode,int i)
{
    int label;
    /*取得标号值*/
    if (i == 1)
	label = midcode.codeR.arg1.midAttr.label;
    else  
        label = midcode.codeR.arg2.midAttr.label; 
  
    /*查找标号地址表*/
    LabelAddr item = labelAddrT;
    LabelAddr last = item;
    while (item!=null)
    {	
        if (item.label == label)
	    break;
	last = item;
	item = item.next;
    }

    if (item==null)  /*表中没有此标号对应得项,填表 */
    {	
	/*预留回填地址*/
	int currentLoc = emitSkip(1);

	/*新建一项*/
	LabelAddr newItem = new LabelAddr();
	newItem.label = label;
	newItem.destNum = currentLoc;
	/*加入标号地址表中*/
	if (last == null)
	    labelAddrT = newItem;
	else 
            last.next = newItem;
    }
    else 
        /*表中有此标号对应项，则可以生成目标代码*/ 
	emitRM("LDC",pc,item.destNum,0,"jump to label");
}
/************************************************/
/* 函数名 jump0Gen				*/ 
/* 功  能 条件跳转语句的目标代码生成		*/		
/* 说  明					*/
/************************************************/
void jump0Gen(CodeFile midcode)
{   
    /*取得决定是否跳转的值，存在ac中*/
    operandGen(midcode.codeR.arg1);
    /*转存到ac2中*/
    emitRM("LDA",ac2,0,ac,"op: store  addr ");  

    /*此处为地址回填预留一个指令空间,生成不跳转时的代码*/
    int savedLoc = emitSkip(1);

    /*生成跳转代码,通过调用*/
    jumpGen(midcode,2);

    /*指令回填*/
    int currentLoc = emitSkip(0);
    emitBackup(savedLoc);
    emitRM_Abs("JNE",ac2,currentLoc,"not jump");
    emitRestore();    
}
/************************************************/
/* 函数名 valactGen				*/ 
/* 功  能 形参为值参时的形实参结合代码生成	*/		
/* 说  明					*/
/************************************************/
void valactGen(CodeFile midcode)
{
    /* 如果代码生成追踪标志TraceCode为TRUE,写入注释,形实参结合开始*/
    if (TraceCode)  
        emitComment("->param  combine ");

    /*取得形参的偏移*/
    int paramoff = midcode.codeR.arg2.midAttr.value;

    /*调用函数，得到实参的值,存在ac中*/
    operandGen(midcode.codeR.arg1);
	
    /*进行形实参结合，在新的AR的对应形参位置写入实参值*/
    emitRM("ST",ac,paramoff,top,"store  param value");

    /* 如果代码生成追踪标志TraceCode为TRUE,写入注释,形实参结合结束*/
    if (TraceCode)  
        emitComment("<-param  combine ");
}
/************************************************/
/* 函数名 varactGen				*/ 
/* 功  能 形参为变参时的代码生成		*/		
/* 说  明					*/
/************************************************/
void varactGen(CodeFile midcode)
{
    /* 如果代码生成追踪标志TraceCode为TRUE,写入注释,形实参结合开始*/
    if (TraceCode)  
        emitComment("->param  combine ");
	
    /*记录参数的偏移*/
    int paramoff = midcode.codeR.arg2.midAttr.value;
   
    /*形参是变参*/
    /*1.实参是直接变量*/
    if (midcode.codeR.arg1.midAttr.addr.access.equals("dir"))
    {   
        /*ac中的地址即为实参地址*/
	/*计算变量的绝对偏移,ac中存为变量的绝对偏移*/
        FindAddr(midcode.codeR.arg1);
    }
    /*2.实参是间接变量*/
    else
    {   
        /*ac中的地址存放的内容为实参地址*/
	/*计算变量的绝对偏移,ac中存为变量的绝对偏移*/
        FindAddr(midcode.codeR.arg1);
	emitRM("LD",ac,0,ac,"");
    }
    
    /*进行形实参结合，在新的AR的对应形参位置写入实参变量的地址*/
    emitRM("ST",ac,paramoff,top,"store  param  var");

    /* 如果代码生成追踪标志TraceCode为TRUE,写入注释,形实参结合结束*/
    if (TraceCode)  
        emitComment("<-param  combine ");	
}					
/************************************************/
/* 函数名 callGen				*/ 
/* 功  能 过程调用处的处理工作			*/		
/* 说  明 为了节约目标代码，将过程调用中的整体  */
/*	  工作分配到三部分，过程调用处，过程入  */
/*	  口处，过程出口处			*/
/************************************************/
void callGen(CodeFile midcode)
{
    /*保存旧的display表的偏移*/
    emitRM("ST",displayOff,6,top," save nOff");
	
    /*新的displayOff的值*/ 
    int Noff = midcode.codeR.arg3.midAttr.value;
    emitRM("LDC",displayOff,Noff,0," new displayOff");

    /*记录下返回地址,注：返回地址只能在这里求得，而且是跳过保存返回
      地址得两条指令，和跳转得一条指令的下一条指令，故加3*/
    int currentLoc = emitSkip(0)+3;
  
    /*保存返回地址*/
    emitRM("LDC",ac,currentLoc,0,"save return address");
    emitRM("ST",ac,1,top,"");

    /*与跳转指令处理相同,注意：跳转写在最后*/
    jumpGen(midcode,1);
}
/************************************************/
/* 函数名 pentryGen				*/ 
/* 功  能 过程体入口处的处理			*/		
/* 说  明 过程入口中间代码中，ARG1是过程入口	*/
/*	  标号，ARG2是diplay表的偏移量，ARG3	*/
/*	  是过程的层数				*/
/************************************************/
void pentryGen(CodeFile midcode)
{
    /*首先处理标号,调用标号处理函数*/
    labelGen(midcode);
    
    /*取出AR大小信息*/
    int ARsize = midcode.codeR.arg2.midAttr.value;

    /*取出过程层数信息*/
    int procLevel = midcode.codeR.arg3.midAttr.value;

    /*保存当前sp*/
    emitRM("ST",sp,0,top,"save old sp");

    /*保存寄存器0,1,2,4*/
    emitRM("ST",ac,3,top,"save ac");
    emitRM("ST",ac1,4,top,"save ac1");
    emitRM("ST",ac2,5,top,"save ac2");
	
    /*保存过程层数*/
    emitRM("LDC",ac,procLevel,0,"save procedure level");
    emitRM("ST",ac,2,top,"");

    /*移display表*/
    for(int ss=0;ss<procLevel;ss++)
    {
	/*取原displayOff,存入ac1中*/
	emitRM("LD",ac1,6,top," fetch old display Off");
	/*ss要加上当前nOff才是对于sp的偏移*/
	emitRM("LDA",ac1,ss,ac1," old display item");
	/*ac1中为绝对地址*/
	emitRO("ADD",ac1,ac1,sp,"");
	/*取当前AR中display表的第ss项,存入ac中*/
	emitRM("LD",ac,0,ac1," fetch display table item");
				
	/*当前AR的displayOff*/
	emitRM("LDA",ac1,ss,displayOff," current display item");
	/*ac1中为绝对地址*/
	emitRO("ADD",ac1,ac1,top,"");
	/*将ac中的内容送入ac1所指地址中*/
	emitRM("ST",ac,0,ac1," send display table item");
    }
    /*在display表中的最上层填写本层的sp*/
    /*ac中存储的为display表最上层的相对off*/
    emitRM("LDA",ac,procLevel,displayOff," current sp in display");
    emitRO("ADD",ac,top,ac," absolute off");
    emitRM("ST",top,0,ac," store address" );

    /*修改sp和top*/
    emitRM("LDA",sp,0,top," new sp value");
    emitRM("LDA",top,ARsize,top,"new top value");
}
/************************************************/
/* 函数名 endprocGen				*/ 
/* 功  能 过程出口处的处理			*/		
/* 说  明					*/
/************************************************/
void endprocGen(CodeFile midcode)
{
    /*恢复寄存器值*/
    emitRM("LD",ac,3,sp,"resume ac");
    emitRM("LD",ac1,4,sp,"resume ac1");
    emitRM("LD",ac2,5,sp,"resume ac2");
    emitRM("LD",displayOff,6,sp,"resume nOff");

    /*恢复sp和top值*/
    emitRM("LDA",top,0,sp,"resume top");
    emitRM("LD",sp,0,sp,"resume sp");

    /*取出返回地址，返回*/		
    emitRM("LD",pc,1,top," load return address");
}
/***********************************************************/
/* 函数名  mentryGen                                       */
/* 功  能  主程序入口的处理部分			           */
/* 说  明  主程序入口中间代码的ARG2记录了主程序AR的display */
/*	   表的偏移；参数savedLoc记录跳转改变pc的指令应在的*/
/*	   位置						   */
/***********************************************************/
void mentryGen(CodeFile midcode,int savedLoc)
{
    /*主程序入口*/
    int currentLoc = emitSkip(0); 
	
    /*回退到目标代码第一条空语句处*/
    emitBackup(savedLoc);
    /*添加指令，将主程序入口地址传至指令寄存器pc*/
    emitRM("LDC",pc,currentLoc,0,"main entry");
    /*恢复当前地址*/
    emitRestore();

    emitComment("-> main procedure");
    /*处理主程序的过程活动记录，需要填写的内容有:全局变量、display表*/   
    /*初始化寄存器*/
    emitRM("LDC",ac,0,0,"initialize ac");
    emitRM("LDC",ac1,0,0,"initialize ac1");
    emitRM("LDC",ac2,0,0,"initialize ac2");

    /*确定sp*/
    emitRM("ST",ac,0,sp," main sp");

    /*确定displayOff*/	
    int Noff = midcode.codeR.arg3.midAttr.value;
    emitRM("LDC",displayOff,Noff,0," new displayOff");

    /*填写display表，只有主程序本层的sp(0)*/
    emitRM("ST",ac,0,displayOff," main display ");

    /*填写top，根据AR的大小*/
    int size = midcode.codeR.arg2.midAttr.value;
    emitRM("LDA",top, size, sp," main top");					
}
/***********************************************************/
/* 函数名  FindAddr                                        */
/* 功  能  计算变量的绝对地址				   */
/* 说  明  将绝对地址存入ac中,注意要保证不用到ac2	   */
/***********************************************************/
void FindAddr( ArgRecord arg)
{
    /*源变量和临时变量处理方式不同*/
    /*记录该变量所在层*/
    int varLevel = arg.midAttr.addr.dataLevel;
    /*记录该变量的相对偏移*/
    int varOff = arg.midAttr.addr.dataOff;
    /*源变量*/
    if (varLevel != -1)
    {			
	/*计算该变量的sp，存入ac中*/
	FindSp(varLevel);
	/*该变量相对于sp的偏移，存入ac1中*/
	emitRM("LDC",ac1,varOff,0," ");
	/* 计算绝对偏移 */
	emitRO("ADD",ac,ac,ac1," var absolute off");
    }
    /*临时变量*/
    else
    {  
        /*临时变量是局部的，只在本AR中有效*/
        /*该变量相对于sp的偏移，存入ac1中*/
	emitRM("LDC",ac1,varOff,0," ");
	/* 计算绝对偏移 */
	emitRO("ADD",ac,sp,ac1," temp absolute off"); 
    }
}
/***********************************************************/
/* 函数名  FindSp                                          */
/* 功  能  找到该变量所在AR的sp,存入ac中                   */
/* 说  明						   */
/***********************************************************/
void FindSp(int varlevel)
{
    /*先求该变量层数在AR中的位置，其中varLevel表示变量所在层*/
    emitRM("LDA",ac,varlevel,displayOff," var process");
	
    /*绝对地址*/
    emitRO("ADD",ac,ac,sp," var sp relative address");
    
    /*该变量所在AR的sp地址存在ac1中*/
    emitRM("LD",ac,0,ac," var sp");
}
/****************************************************************/
/* 函数名 emitComment						*/
/* 功  能 注释生成函数						*/
/* 说  明 该函数将函数参数c指定的注释内容写入代码文件code	*/
/****************************************************************/
void emitComment(String c)
/* 如果代码生成追踪标志TraceCode为TRUE,将注释写入目标代码文件 */
{
    if (TraceCode) 
	mbcode=mbcode+"* "+c+"\n";
}
/********************************************************/
/* 函数名 emitRO					*/
/* 功  能 寄存器地址模式指令生成函数			*/
/* 说  明 该函数产生一条只用寄存器操作数的TM指令	*/
/*	  op 为操作码;				        */
/*	  r  为目标寄存器;				*/
/*	  s  第一源寄存器;				*/
/*	  t  第二源寄存器;				*/
/*        c  为将写入代码文件code的注释内容		*/
/********************************************************/
void emitRO(String op,int r,int s,int t,String c)
{  
             /* 将TM指令格式化写入代码文件,当前生成代码写入地址emitLoc加1 */
             mbcode=mbcode+String.valueOf(emitLoc++)+":"+op+"  "+String.valueOf(r)+","+String.valueOf(s)+","+String.valueOf(t);

             /* 如果代码生成追踪标志TraceCode为TRUE,将注释c写入代码文件 */
             if (TraceCode)    
	         mbcode=mbcode+"\t"+"*"+c;
             /* 一条代码指令写完,加入代码行结束标志 */
             mbcode=mbcode+"\n";
    /* 当前生成代码写入地址若超出最高生成代码写入地址						       改变最高生成代码写入地址highEmitLoc为当前生成代码写入地址emitLoc */
    if (highEmitLoc<emitLoc) 
        highEmitLoc = emitLoc ;
} 
/********************************************************/
/* 函数名 emitRM					*/
/* 功  能 变址地址模式指令生成函数			*/
/* 说  明 该函数产生一条寄存器-内存操作数TM指令		*/
/*	  op 操作码;					*/
/*	  r  目标寄存器;				*/
/*	  d  为偏移值;					*/
/*        s  为基地址寄存器;				*/
/*	  c  为将写入代码文件code的注释内容		*/
/********************************************************/
void emitRM(String op,int r,int d,int s,String c)
{ 
             /* 将TM指令格式化写入代码文件,当前生成代码写入地址emitLoc加1 */
             mbcode=mbcode+String.valueOf(emitLoc++)+":"+op+"  "+String.valueOf(r)+","+String.valueOf(d)+"("+String.valueOf(s)+")";

             /* 如果代码生成追踪标志TraceCode为TRUE,将注释c写入代码文件 */
             if (TraceCode)    
	         mbcode=mbcode+"\t"+"*"+c;
             /* 一条代码指令写完,加入代码行结束标志 */
             mbcode=mbcode+"\n";
    /* 当前生成代码写入地址若超出最高生成代码写入地址						       改变最高生成代码写入地址highEmitLoc为当前生成代码写入地址emitLoc */
    if (highEmitLoc<emitLoc) 
        highEmitLoc = emitLoc ;
} 
/****************************************************/
/* 函数名 emitSkip				    */	
/* 功  能 空过生成函数				    */
/* 说  明 该函数空过howMany指定数量的写入代码位置,  */
/*	  返回当前生成代码写入地址	            */
/****************************************************/
int emitSkip(int howMany)
{  
    /* 当前生成代码写入地址emitLoc赋给变量i */
    int i = emitLoc;

    /* 新的当前生成代码写入地址emitLoc略过howMany指定数量的写入指令位置 */
    emitLoc = emitLoc+howMany ;

    /* 若当前生成代码写入地址emitLoc超过最高生成代码写入地址highEmitLoc	
       更新最高生成代码写入地址highEmitLoc	*/
    if (highEmitLoc < emitLoc)  
        highEmitLoc = emitLoc ;

    /* 函数返回旧的当前生成代码写入地址i */
    return i;
} 
/********************************************************/
/* 函数名 emitBackup					*/
/* 功  能 地址回退函数					*/	
/* 说  明 该函数退回到以前被空过的生成代码写入地址loc	*/
/********************************************************/
void emitBackup(int loc)
{
   /* 如果要退回的地址loc比当前最高地址highEmitLoc还高	
      报退回错误,将错误信息作为注释写入代码文件code	*/
   if (loc > highEmitLoc) 
       emitComment("BUG in emitBackup");

   /* 更新当前生成代码写入地址emitLoc为函数参数loc,完成退回动作 */
   emitLoc = loc ;
} 
/********************************************************/
/* 函数名 emitRestore					*/
/* 功  能 地址恢复函数					*/
/* 说  明 该函数将当前生成代码写入地址emitLoc恢复为	*/
/*	  当前未写入指令的最高地址highEmitLoc	        */
/********************************************************/
void emitRestore()
{ 
    emitLoc = highEmitLoc;
}
/************************************************/
/* 函数名 emitRM_Abs				*/
/* 功  能 地址转换函数				*/
/* 说  明 该函数在产生一条寄存器-内存TM指令时,	*/
/*	  将绝对地址参数转换成pc相对地址参数	*/
/*	  op 为操作码;				*/
/*        r  为目标寄存器;			*/
/*	  a  为存储器绝对地址;			*/
/*	  c  为将写入代码文件code的注释		*/
/************************************************/
void emitRM_Abs(String op,int r,int a,String c)
{  
             /* 将TM指令格式化写入代码文件,将函数参数a给定的绝对地址
                转换为相对于指令指示器pc的相对地址a-(emitLoc+1) */
             mbcode=mbcode+String.valueOf(emitLoc)+":"+op+"  "+String.valueOf(r)+","+String.valueOf(a-(emitLoc+1))+"("+String.valueOf(pc)+")";

             /* 更新当前生成代码写入地址emitLoc */
             ++emitLoc;

             /* 如果代码生成追踪标志TraceCode为TRUE,将注释c写入代码文件 */
             if (TraceCode)    
	         mbcode=mbcode+"\t"+"*"+c;
             /* 一条代码指令写完,加入代码行结束标志 */
             mbcode=mbcode+"\n";
    /* 当前生成代码写入地址若超出最高生成代码写入地址						       改变最高生成代码写入地址highEmitLoc为当前生成代码写入地址emitLoc */
    if (highEmitLoc<emitLoc) 
        highEmitLoc = emitLoc;
} 
}

/********************************************************************/
/* 类  名 Opt	                                                    */
/* 功  能 总程序的处理					            */
/* 说  明 建立一个类，处理总程序                                    */
/********************************************************************/
class Opt
{
CodeFile baseBlock[]=new CodeFile[100];
int blocknum;

/*临时变量编号，全局变量,每个过程开始都对TempOffset进行
  初始化，注：所以，不同过程中，可能有编号相同的临时变量，但是
  由于他们互不相干，所以不会有问题；而且优化部分是对基本块进行优化，
  每个基本块最大是一个过程，也不会有问题 */
int TempOffset=0;

/*标号值，全局变量*/
int Label=0;

/*指向第一条中间代码*/
CodeFile firstCode;

/*指向当前最后一条中间代码*/
CodeFile lastCode;

/*记录主程序display表的偏移量*/
int StoreNoff;

/*常量定值表*/
ConstDefT table;

ValuNum valuNumT;
UsableExpr usableExprT;
TempEqua tempEquaT;
/*记录值编码*/
int Vnumber=0;

/*变量定值表,用变量的arg结构表示变量*/
ArgRecord varTable[] = new ArgRecord[100];
int TotalNum = 0;

/*循环信息栈*/
LoopStack loopTop=null;
boolean loopStackEmpty;

int mainOff;
boolean Error=false;
boolean Error1=false;
String yerror;
String serror;
CodeFile mid;

public Opt(String s)
{
    AnalYuyi a=new AnalYuyi(s);
    StoreNoff=a.StoreNoff;
    mainOff=a.mainOff;
    if (a.Error1)
    {
        Error1=true;
        serror=a.serror;
    }
    else if (a.Error)
    {
        Error=true;
        yerror=a.yerror;
    }
    else
    {
        mid=GenMidCode(a.yuyiTree);
        ConstOptimize();
        ECCsave();
        LoopOpti();
    }
}
 
/********************************************************/
/* 函数名  GenMidCode 	  				*/
/* 功  能  中间代码生成主函数				*/
/* 说  明  若有过程声明，调用过程声明的代码声明函数；   */
/*         调用程序体的代码生成函数			*/
/********************************************************/
CodeFile GenMidCode(TreeNode t)
{  
    /*若有过程声明，调用相应函数，产生过程声明的中间代码*/
    TreeNode t1=t.child[1];
    while (t1!=null)
    { 
        if (t1.nodekind.equals("ProcDecK"))
            GenProcDec(t1);
        t1=t1.sibling; 
    }
  
    /*display表相对于sp的偏移*/
    ArgRecord Noff = ARGValue(StoreNoff);

    /*生成主程序入口声明代码*/
    CodeFile code = GenCode("MENTRY",null,null,Noff);

    /*初始化临时变量的开始编号,为临时变量区的第一个地址*/
    TempOffset = StoreNoff + 1;

    /*调用语句序列的代码生成函数*/
    GenBody(t.child[2]);

    /*回填主程序的AR的大小到主程序入口中间代码*/
    int size = TempOffset;
    ArgRecord sizeArg = ARGValue(size);
    code.codeR.arg2= sizeArg;

    return firstCode;
}
/****************************************************/
/* 函数名  GenProcDec				    */
/* 功  能  过程声明中间代码生成函数		    */
/* 说  明  生成过程入口中间代码，生成过程体的中间   */
/*	   代码，生成过程出口的中间代码		    */	
/****************************************************/
void GenProcDec(TreeNode t)
{  
    /*得到过程的入口标号*/
    int ProcEntry = NewLabel();
  
    /*过程名在符号表中的地址*/
    SymbTable Entry = t.table[0];
    /*过程入口标号，回填入节点中*/
    Entry.attrIR.proc.codeEntry = ProcEntry;

    /*过程的display表的偏移量*/
    int noff = Entry.attrIR.proc.nOff;
 
    /*得到过程的层数及其ARG结构*/
    int procLevel = Entry.attrIR.proc.level;
    ArgRecord levelArg = ARGValue(procLevel);
  
    /*若过程内部仍有过程声明，调用相应函数，产生过程声明的中间代码*/
    TreeNode t1=t.child[1];
    while (t1!=null)
    { 
        if (t1.nodekind.equals("ProcDecK"))
            GenProcDec(t1);
        t1=t1.sibling; 
    }

    /*产生过程入口中间代码*/ 
    ArgRecord arg1 = ARGLabel(ProcEntry);
    CodeFile code = GenCode("PENTRY",arg1,null,levelArg);
  
    /*初始化临时变量的开始编号,为过程临时变量区的第一个地址*/
    TempOffset =  noff + procLevel+1;

    /*调用语句序列的代码生成函数处理过程体*/
    GenBody(t.child[2]);

    /*得到过程的AR的大小,回填入过程入口中间代码*/
    int size = TempOffset;
    ArgRecord sizeArg = ARGValue(size);
    code.codeR.arg2 = sizeArg;

    /*产生过程出口中间代码*/
    GenCode("ENDPROC",null,null,null);
}
/****************************************************/
/* 函数名  GenBody				    */
/* 功  能  语句序列中间代码生成函数		    */
/* 说  明  用于处理过程体或者程序体，		    */
/*	   循环处理各个语句			    */	
/****************************************************/
void GenBody(TreeNode t)
{  
    TreeNode t1 = t;
    /*令指针指向第一条语句*/
    if (t1.nodekind.equals("StmLK"))
	t1=t1.child[0];

   while (t1!=null)
   { 
       /*调用语句处理函数*/
       GenStatement(t1);
       t1= t1.sibling;
   }
}
/****************************************************/
/* 函数名  GenStatement				    */
/* 功  能  语句处理函数	        		    */
/* 说  明  根据语句的具体类型，分别调用相应的	    */
/*	   语句处理函数				    */
/****************************************************/
void GenStatement(TreeNode t) 
{  
    if (t.kind.equals("AssignK"))
	GenAssignS(t);  
    else if (t.kind.equals("CallK"))
	GenCallS(t);   
    else if (t.kind.equals("ReadK"))     
        GenReadS(t);   
    else if (t.kind.equals("WriteK"))     
        GenWriteS(t);	
    else if (t.kind.equals("IfK"))       
        GenIfS (t);	   
    else if (t.kind.equals("WhileK"))  	
        GenWhileS(t);		
    else if (t.kind.equals("ReturnK"))    /*直接生成中间代码*/  	
        GenCode("RETURNC",null,null,null);
}
/****************************************************/
/* 函数名  GenAssignS				    */
/* 功  能  赋值语句处理函数        	            */
/* 说  明  处理左部变量，处理右部表达式，生成       */
/*         赋值语句中间代码 			    */
/****************************************************/
void GenAssignS(TreeNode t)
{   
    /*调用赋值左部变量的处理函数*/
    ArgRecord Larg = GenVar(t.child[0]);
    /*调用赋值右部表达式的处理函数*/
    ArgRecord Rarg = GenExpr(t.child[1]);
    /*生成赋值语句中间代码*/
    GenCode("ASSIG",Rarg,Larg,null);
}
/****************************************************/
/* 函数名  GenVar				    */
/* 功  能  变量处理函数        		            */
/* 说  明					    */
/****************************************************/
ArgRecord GenVar(TreeNode t)
{ 
    int low,size;
    FieldChain head;

    /*生成变量名的ARG结构, Entry为标识符在符号表中的地址*/
    SymbTable Entry = t.table[0];
    ArgRecord V1arg = ARGAddr(t.name[0],Entry.attrIR.var.level,
Entry.attrIR.var.off,Entry.attrIR.var.access);
    
    /*返回的ARG结构*/
    ArgRecord Varg=null;
    if (t.attr.expAttr.varkind.equals("IdV"))
        /*标识符变量情形*/
 	Varg = V1arg; 
    else if (t.attr.expAttr.varkind.equals("ArrayMembV"))
    {    
	/*数组成员变量情形*/
	/*构造数组下届和数组大小的ARG结构*/
	low = Entry.attrIR.idtype.array.low;
	size = Entry.attrIR.idtype.array.elementTy.size;
        Varg = GenArray(V1arg,t,low,size);
    }
    else if (t.attr.expAttr.varkind.equals("FieldMembV"))
    {
        /*域变量情形*/    
	head = Entry.attrIR.idtype.body;
	Varg = GenField(V1arg,t,head);
    }
    return Varg;
}
/****************************************************/
/* 函数名  GenArray				    */
/* 功  能  数组成员变量处理函数    		    */
/* 说  明  由函数GenVar或函数GenField调用	    */
/****************************************************/      	  
ArgRecord GenArray(ArgRecord V1arg,TreeNode t,int low,int size)  		  
{   
    /*处理下标表达式*/
    ArgRecord Earg= GenExpr(t.child[0]);

    ArgRecord lowArg = ARGValue(low);
    ArgRecord sizeArg= ARGValue(size);
    /*产生三个临时变量*/
    ArgRecord temp1= NewTemp("dir");
    ArgRecord temp2= NewTemp("dir");
    /*注：表示复杂变量的临时变量属于间接访问*/
    ArgRecord temp3= NewTemp("indir"); 
	      
    /*生成中间代码*/
    GenCode("SUB", Earg, lowArg ,temp1);
    GenCode("MULT",temp1,sizeArg,temp2);
    GenCode("AADD",V1arg,temp2, temp3);

    return temp3;
}		  		  		  
/****************************************************/
/* 函数名  GenField				    */
/* 功  能  域变量处理函数    			    */
/* 说  明  由函数GenVar调用			    */
/****************************************************/		  
ArgRecord GenField(ArgRecord V1arg,TreeNode t,FieldChain head)		  
{   
    ArgRecord FieldV;
    /*t1指向当前域成员*/
    TreeNode t1 = t.child[0];
    FieldChain Entry2=new FieldChain();

    FindField(t1.name[0],head,Entry2);
    /*域名在域表中的偏移*/
    int off = Entry2.off;
    ArgRecord offArg = ARGValue(off);
    /*注：表示复杂变量的临时变量属于间接访问*/
    ArgRecord temp1 = NewTemp("indir");
    GenCode("AADD",V1arg,offArg,temp1);
    /*域是数组变量*/
    if (t1.attr.expAttr.varkind.equals("ArrayMembV"))
    {  
        int low = Entry2.unitType.array.low;
 	int size= Entry2.unitType.array.elementTy.size;
	FieldV = GenArray(temp1,t1,low,size);
    }
    else  /*域是标识符变量*/
   	FieldV = temp1;

    return FieldV;
}
/****************************************************/
/* 函数名  GenExpr				    */
/* 功  能  表达式处理函数        		    */
/* 说  明					    */
/****************************************************/
ArgRecord GenExpr(TreeNode t)
{ 
    ArgRecord arg=null;
    ArgRecord Larg;
    ArgRecord Rarg;
    ArgRecord temp;

    if (t.kind.equals("VariK"))
        arg = GenVar(t);
    else if (t.kind.equals("ConstK"))
	/*得到值的ARG结构*/
	arg = ARGValue(t.attr.expAttr.val);
    else if (t.kind.equals("OpK"))
    {
	/*处理左部和右部*/
	Larg = GenExpr(t.child[0]);
	Rarg = GenExpr(t.child[1]);

	/*根据操作符类别，得到中间代码的类别*/
	String op=null;
	if (t.attr.expAttr.op.equals("LT"))
	    op = "LTC"; 
	else if (t.attr.expAttr.op.equals("EQ"))	
            op = "EQC"; 
	else if (t.attr.expAttr.op.equals("PLUS"))		
            op = "ADD";
	else if (t.attr.expAttr.op.equals("MINUS"))   
            op = "SUB"; 
	else if (t.attr.expAttr.op.equals("TIMES"))    
            op = "MULT"; 
	else if (t.attr.expAttr.op.equals("OVER"))    
            op = "DIV"; 
	/*产生一个新的临时变量*/	
        temp = NewTemp("dir");
	/*生成中间代码*/
	GenCode(op,Larg,Rarg,temp);
	arg = temp ;
    }
    return arg;
}
/****************************************************/
/* 函数名  GenCall				    */
/* 功  能  过程调用处理函数        		    */
/* 说  明  分别调用表达式处理函数处理各个实参，并   */
/*	   生成相应的形实参结合中间代码；从符号表中 */
/*	   过程标识符属性中，查到入口标号，产生过程 */
/*	   调用中间代码				    */
/****************************************************/
void GenCallS(TreeNode t)
{
    /*取得过程标志符在符号表中的地址*/
    SymbTable Entry = t.child[0].table[0];
    ParamTable param = Entry.attrIR.proc.param;
    /*调用表达式处理函数处理各个实参，
      并生成相应的形实参结合中间代码*/
    TreeNode t1 = t.child[1];
    ArgRecord Earg;
    while (t1!=null)
    { 
        Earg = GenExpr(t1);

        /*记录参数的偏移*/
        int paramOff = param.entry.attrIR.var.off;
	ArgRecord OffArg = ARGValue(paramOff);
        /*形实参结合中间代码*/
	if (param.entry.attrIR.var.access.equals("dir")) 
	    /*值参结合中间代码*/
            GenCode("VALACT",Earg,OffArg,null);
	else  /*变参结合中间代码*/
	    GenCode("VARACT",Earg,OffArg,null);
	
	t1 = t1.sibling;
        param = param.next;
    } 
    /*过程入口标号及其ARG结构*/
    int label = Entry.attrIR.proc.codeEntry;
    ArgRecord labelarg = ARGLabel(label);
   
    /*过程的display表的偏移量*/
    int Noff = Entry.attrIR.proc.nOff;
    ArgRecord Noffarg = ARGValue(Noff);

    /*生成过程调用中间代码*/
    GenCode ("CALL",labelarg,null,Noffarg);
}
/****************************************************/
/* 函数名  GenReadS				    */
/* 功  能  读语句处理函数        		    */
/* 说  明  得到读入变量的ARG结构，生成读语句中间代码*/
/****************************************************/
void GenReadS(TreeNode t)
{ 
    SymbTable Entry = t.table[0];
    ArgRecord Varg = ARGAddr(t.name[0],Entry.attrIR.var.level,
Entry.attrIR.var.off,Entry.attrIR.var.access);
    /*生成读语句中间代码*/
    GenCode("READC",Varg,null,null);
}
/****************************************************/
/* 函数名  GenWrite				    */
/* 功  能  写语句处理函数        		    */
/* 说  明  调用表达式的中间代码生成函数，并产生写   */
/*	   语句的中间代码			    */
/****************************************************/
void GenWriteS(TreeNode t)
{   
    /*调用表达式的处理*/
    ArgRecord Earg = GenExpr(t.child[0]);
    /*生成写语句中间代码*/
    GenCode("WRITEC",Earg,null,null);
}
/****************************************************/
/* 函数名  GenIfs				    */
/* 功  能  条件语句处理函数        	            */
/* 说  明					    */
/****************************************************/
void GenIfS(TreeNode t)
{   
    /*生成else部分入口标号，及其ARG结构*/
    int elseL = NewLabel();
    ArgRecord ElseLarg=ARGLabel(elseL);

    /*生成if语句出口标号，及其ARG结构*/
    int outL = NewLabel();
    ArgRecord OutLarg = ARGLabel(outL);

    /*条件表达式的中间代码生成*/
    ArgRecord Earg = GenExpr(t.child[0]);

    /*若表达式为假，跳转至else入口标号*/
    GenCode("JUMP0",Earg,ElseLarg,null);
    
    /*then部分中间代码生成*/
    GenBody(t.child[1]);
    
    /*跳到if出口*/
    GenCode("JUMP",OutLarg,null,null);

    /*else部分入口标号声明*/
    GenCode("LABEL",ElseLarg,null,null);

    /*else部分中间代码生成*/
    GenBody(t.child[2]);

    /*if语句出口标号声明*/
    GenCode("LABEL",OutLarg,null,null);
}
/****************************************************/
/* 函数名  GenWhileS				    */
/* 功  能  循环语句处理函数        		    */
/* 说  明  将循环入口和出口用不同的中间代码标志，是 */
/*	   为了循环不变式外提的需要		    */
/****************************************************/
void GenWhileS(TreeNode t)
{   
    /*生成while语句入口标号，及其ARG结构*/
    int inL = NewLabel() ;
    ArgRecord InLarg = ARGLabel(inL);

    /*生成while语句出口标号，及其ARG结构*/
    int outL = NewLabel();
    ArgRecord OutLarg = ARGLabel(outL);

    /*while语句入口标号声明*/
    GenCode("WHILESTART",InLarg,null,null);
    
    /*条件表达式的中间代码生成*/
    ArgRecord Earg = GenExpr(t.child[0]);

    /*若表达式为假，跳转至while语句出口*/
    GenCode("JUMP0",Earg,OutLarg,null);
    
    /*循环体中间代码生成*/
    GenBody(t.child[1]);
    
    /*跳到while入口*/
    GenCode("JUMP",InLarg,null,null);

    /*while出口标号声明*/
    GenCode("ENDWHILE",OutLarg,null,null);
}
/********************************************************/
/* 函数名  NewTemp		  			*/
/* 功  能  产生一个新的临时变量的ARG结构		*/
/* 说  明  临时变量的层数为-1，偏移为编号值，访问方式由 */
/*	   参数确定					*/
/********************************************************/
ArgRecord NewTemp(String access)
{  
    ArgRecord newTemp=new ArgRecord();
    /*填写临时变量的ARG内容*/
   
    newTemp.form="AddrForm";
    newTemp.midAttr.addr=new Addr();
    newTemp.midAttr.addr.dataLevel=-1 ;
    newTemp.midAttr.addr.dataOff=TempOffset ;
    newTemp.midAttr.addr.access=access;
    /*临时变量编号加1*/   
    TempOffset++;
      
    return newTemp;
}
/********************************************************/
/* 函数名  NewLabel		  			*/
/* 功  能  产生一个新的标号值				*/
/* 说  明  通过全局变量Label加1，产生新的标号值		*/
/********************************************************/
int NewLabel()
{  
    Label++;  
    return Label;
}
/********************************************************/
/* 函数名  ARGAddr		  			*/
/* 功  能  对于给定的变量产生相应的ARG结构		*/
/* 说  明  						*/
/********************************************************/
ArgRecord ARGAddr(String id,int level,int off,String access)
{   
    ArgRecord arg = new ArgRecord();
    /*填写变量ARG结构的内容*/
    arg.form = "AddrForm";
    arg.midAttr.addr=new Addr();
    arg.midAttr.addr.name=id;
    arg.midAttr.addr.dataLevel=level;
    arg.midAttr.addr.dataOff=off;
    arg.midAttr.addr.access=access;
		  
    return arg;
}
/********************************************************/
/* 函数名  ARGLabel		  			*/
/* 功  能  对于给定的标号产生相应的ARG结构		*/
/* 说  明  						*/
/********************************************************/
ArgRecord ARGLabel(int label)
{  
    ArgRecord arg = new ArgRecord();
    arg.form = "LabelForm";
    arg.midAttr.label = label;

    return arg;
}
/********************************************************/
/* 函数名  ARGValue		  			*/
/* 功  能  对于给定的常数值产生相应的ARG结构	        */
/* 说  明  						*/
/********************************************************/
ArgRecord ARGValue(int value)
{ 
    ArgRecord arg = new ArgRecord();
    arg.form = "ValueForm";
    arg.midAttr.value = value;

    return arg;
}
/********************************************************/
/* 函数名  GenCode 		  			*/
/* 功  能  根据给定参数，构造一条中间代码		*/
/* 说  明						*/
/********************************************************/
CodeFile GenCode(String codekind,ArgRecord Arg1,ArgRecord Arg2,ArgRecord Arg3)
{ 
    CodeFile newCode = new CodeFile();
    /*填写代码的内容*/	
    newCode.codeR.codekind = codekind;
    newCode.codeR.arg1 = Arg1;  
    newCode.codeR.arg2 = Arg2;
    newCode.codeR.arg3 = Arg3;
    /*链入中间代码表中*/
    if (firstCode==null)
	firstCode = newCode;
    else
    {	   
        lastCode.next = newCode;
	newCode.former = lastCode;
    }
    lastCode = newCode;

    return newCode;
}
/********************************************************/
/* 函数名  FindField	  				*/
/* 功  能  查找纪录的域名				*/
/* 说  明  返回值为是否找到标志，变量Entry返回此域名在  */
/*	   纪录的域表中的位置.			        */
/********************************************************/
boolean FindField(String Id,FieldChain head,FieldChain Entry)
{ 
    boolean  present=false;
    /*记录当前节点*/
    FieldChain currentItem = head;
    /*从表头开始查找这个标识符，直到找到或到达表尾*/
    while ((currentItem!=null)&&(!present))
    { 
        if  (currentItem.id.equals(Id)) 
	{ 
            present=true;
	    if (Entry!=null)
            {
	        Entry.id=currentItem.id;
                Entry.off=currentItem.off;
                Entry.unitType=currentItem.unitType;
                Entry.next=currentItem.next;
            }
        }
        else  
            currentItem=currentItem.next;
    }
    return present;
}

/****************************************************/
/* 函数名  ConstOptimize			    */
/* 功  能  常表达式优化主函数      		    */
/* 说  明  循环对各个基本块进行常表达式优化	    */
/****************************************************/
CodeFile ConstOptimize()
{    
    /*调用划分基本块函数*/
    blocknum = DivBaseBlock();

    /*循环对每个基本块进行常表达式优化*/
    for (int i=0;i<blocknum;i++)
    {  
        /*基本块入口处置常量定值表为空*/
	table = null;
	/*基本块的常表达式优化*/
        OptiBlock(i);
    }
    /*返回优化后的中间代码*/
    return firstCode;
}
/****************************************************/
/* 函数名  OptiBlock				    */
/* 功  能  对一个基本块进行常表达式优化		    */
/* 说  明					    */
/****************************************************/
void OptiBlock(int i)
{   
    boolean delCode;
    /*指向基本块第一条语句*/
    CodeFile currentCode = baseBlock[i] ;
    CodeFile formerCode;
    CodeFile laterCode;
	    
    ArgRecord arg1;
    ArgRecord arg2;

    /*循环处理每条代码，直到当前基本块结束*/
    while ((currentCode!=baseBlock[i+1])&&(currentCode!=null))
    {   
        if ((currentCode.codeR.codekind.equals("ADD"))||(currentCode.codeR.codekind.equals("SUB"))||(currentCode.codeR.codekind.equals("MULT"))||(currentCode.codeR.codekind.equals("DIV"))||(currentCode.codeR.codekind.equals("LTC"))||(currentCode.codeR.codekind.equals("EQC")))
	{ 
            /*算术和关系操作*/ 
	    /*调用算术和关系运算处理函数*/ 
	    delCode = ArithC(currentCode);
	    /*删除标识为真时，删除当前多元式*/
            if (delCode)
	    {  
                formerCode = currentCode.former;
		laterCode = currentCode.next;
		if (formerCode!=null)
		    formerCode.next = laterCode;
		if (laterCode!=null)
		    laterCode.former = formerCode; 
		currentCode = formerCode;
	    }
        }		
        else if (currentCode.codeR.codekind.equals("ASSIG"))
        {
	    /*对第一个ARG结构进行值替换*/
	    SubstiArg(currentCode,1);
	    arg1 = currentCode.codeR.arg1;
	    arg2 = currentCode.codeR.arg2;
	    /*若是常数结构，则将常量定值加入常量定值表*/
	    if (arg1.form.equals("ValueForm"))
		AppendTable(arg2,arg1.midAttr.value);
	    else  /*删除表中含有此变量的定值*/ 
		DelConst(arg2);
	}
        else if (currentCode.codeR.codekind.equals("JUMP0")||currentCode.codeR.codekind.equals("WRITEC"))
	    /*对第一个ARG结构进行值替换*/
	    SubstiArg(currentCode,1);
        else if (currentCode.codeR.codekind.equals("AADD"))
	    /*对第二个ARG结构进行值替换*/
	    SubstiArg(currentCode,2);

	/*令指针指向下一条代码*/
	currentCode = currentCode.next;
    }
}
/****************************************************/
/* 函数名  ArithC				    */
/* 功  能  处理算术操作和关系比较操作		    */
/* 说  明  对运算分量1和运算分量2进行值替换，若都是 */
/*	   常数，将结果写入常量定值表，并置四元式   */
/*	   删除标志为真				    */
/****************************************************/
boolean ArithC(CodeFile code)
{
    boolean delCode = false;
    int value1,value2,result=0;
    /*对分量1进行值替换*/
    SubstiArg(code,1);
    ArgRecord arg1 = code.codeR.arg1;

    /*对分量2进行值替换*/
    SubstiArg(code,2);
    ArgRecord arg2 = code.codeR.arg2;

    String codekind =code.codeR.codekind;
    ArgRecord arg3 = code.codeR.arg3;

    /*操作分量都是常数*/
    if ((arg1.form.equals("ValueForm"))&&(arg2.form.equals("ValueForm")))
    {   
        value1 = arg1.midAttr.value;
	value2 = arg2.midAttr.value;
	if (codekind.equals("ADD"))
            result = value1+value2;
        else if (codekind.equals("SUB"))
            result = value1-value2;
        else if (codekind.equals("MULT"))
            result = value1*value2;
        else if (codekind.equals("DIV"))
            result = value1/value2;	
        else if (codekind.equals("LTC"))
        {   
            if (value1<value2)
		result = 1;
	    else  
                result = 0;
	}
        else if (codekind.equals("EQC"))   
        {
            if (value1==value2)
		result = 1;
	    else 
                result = 0; 
        } 
	/*操作结果写入常量定值表*/
	AppendTable(arg3,result);
	/*当前多元式应删除*/
        delCode = true;
    }
    return delCode;
}
/****************************************************/
/* 函数名  SubstiArg				    */
/* 功  能  对一个ARG结构进行值替换		    */
/* 说  明  参数i指出对中间代码的哪个ARG结构进行替换 */
/****************************************************/
void SubstiArg(CodeFile code,int i)
{   
    ConstDefT Entry=new ConstDefT();
    ArgRecord arg;
    ArgRecord newArg;

    if (i==1)
	arg = code.codeR.arg1;
    else 
        arg = code.codeR.arg2;
    /*若ARG结构是地址类，且常量定值表中有定值，则值替换*/
    if (arg.form.equals("AddrForm"))
    {  
	boolean constflag = FindConstT(arg,Entry);
	if (constflag)
	{ 
            /*创建一个值的ARG结构，替换原有的ARG结构*/
	    newArg = new ArgRecord();
	    newArg.form = "ValueForm";
	    newArg.midAttr.value = Entry.constValue;
	    if (i==1)
		code.codeR.arg1 = newArg;
	    else 
                code.codeR.arg2 = newArg;
        }
    }
}
/****************************************************/
/* 函数名  FindConstT				    */
/* 功  能  在常量定值表中查找当前变量是否有定值	    */
/* 说  明  输入为变量的ARG结构，根据变量是临时变量  */
/*	   还是一般标识符变量，分别处理		    */
/****************************************************/
boolean FindConstT(ArgRecord arg,ConstDefT Entry)
{ 
    boolean present = false;

    int level = arg.midAttr.addr.dataLevel;
    int off = arg.midAttr.addr.dataOff;
	
    ConstDefT t = table;
    while((t!=null)&&(!present))
    {
        if ((t.variable.midAttr.addr.dataLevel==level)
&&(t.variable.midAttr.addr.dataOff==off))
	{	
            present = true;
            /*须逐一赋值，不可直接写为Entry=t*/
            Entry.constValue = t.constValue;
	}
	t = t.next;
    }
    return present;
}
/****************************************************/
/* 函数名  AppendTable				    */
/* 功  能  将变量和其常量值写入常量定值表	    */
/* 说  明  创建一个新的节点，填写常量定值内容，并连 */
/*	   入表中				    */
/****************************************************/
void AppendTable(ArgRecord arg,int result)
{ 
    ConstDefT last = table;
    ConstDefT current = table;
    ConstDefT Entry = new ConstDefT();
    /*查找，若已存在此变量，则改变其值*/
    boolean present =  FindConstT(arg,Entry);
    if (present)
	Entry.constValue = result;
    else
    {	
        /*否则，创建一个新的节点*/
	ConstDefT newConst = new ConstDefT();
	newConst.constValue = result;
        newConst.variable = arg;

	/*当前节点加入常量定值表中*/
	if (table==null)
	    table = newConst;
	else 
	{	 
            while (last.next!=null)
	        last = last.next;
	    last.next = newConst;
	    newConst.former = last ;
	}	
    }
}

/****************************************************/
/* 函数名  DelConst				    */
/* 功  能  删除一个常量定值			    */
/* 说  明  若存在，则从常量定值表中删除，否则结束   */
/****************************************************/
void DelConst(ArgRecord arg)
{   
    ConstDefT Entry = new ConstDefT();
    ConstDefT former;
    ConstDefT later;
    /*查找变量,若存在则删除；否则，结束*/
    boolean present =  FindConstT(arg,Entry) ;
    if (present)
    {	
        former = Entry.former;
	later = Entry.next;
        if (former!=null)
	    former.next = later;
        if (later!=null)
	    later.former = former;
    }
}
/********************************************************/
/* 函数名  DivBaseBlock					*/
/* 功  能  为中间代码划分基本块				*/
/* 说  明  基本块从0开始编号，若有变参传递，则相应过程  */
/*	   调用做为当前基本块的结束			*/
/********************************************************/
int DivBaseBlock()
{   
    /*初始化基本块数目*/
    int blocknum = 0;	
    CodeFile code = firstCode;

    while (code!=null)
    {
	if ((code.codeR.codekind.equals("LABEL"))||(code.codeR.codekind.equals("WHILESTART"))||(code.codeR.codekind.equals("PENTRY"))||(code.codeR.codekind.equals("MENTRY")))
        {
	    /*进入一个新的基本块*/
            baseBlock[blocknum] =code;
            blocknum++;
        } 
	else if ((code.codeR.codekind.equals("JUMP"))||(code.codeR.codekind.equals("JUMP0"))||(code.codeR.codekind.equals("RETURNC"))||(code.codeR.codekind.equals("ENDPROC"))||(code.codeR.codekind.equals("ENDWHILE")))
        {
	    /*从下一条语句开始，进入一个新的基本块*/
	    if (code.next!=null)
	    { 
                code = code.next;
		baseBlock[blocknum] =code;
                blocknum++;
	    }
	}
	else if (code.codeR.codekind.equals("VARACT"))
	{ 
	    /*找到对应的过程调用语句，作为本基本块的结束*/
	    code = code.next;
	    while (!(code.codeR.codekind.equals("CALL")))
		code = code.next;
	    /*从下一条语句开始，进入一个新的基本块*/
	    if (code.next!=null)
	    { 
                code = code.next;
		baseBlock[blocknum] =code;
                blocknum++;
	    }			 
        }
        code = code.next;
    }
    return blocknum;
}

/****************************************************/
/* 函数名  ECCsave				    */
/* 功  能  公共表达式优化主函数    		    */
/* 说  明  循环对各个基本块进行公共表达式优化	    */
/****************************************************/
/*注：考虑将blocknum用作局部变量，全局变量封装不好*/
CodeFile ECCsave()
{ 
    /*循环对每个基本块进行公共表达式优化*/
    for (int i=0 ;i<blocknum;i++)
    {  
        /*基本块入口处置值编码表，
	  可用表达式表，临时变量等价表为空*/
	valuNumT = null;
	usableExprT = null;
	tempEquaT = null;

	/*基本块的ECC节省*/
        SaveInBlock(i);
    }
    /*返回优化后的中间代码*/
    return firstCode;
}
/****************************************************/
/* 函数名  SaveInBlock				    */
/* 功  能  基本块优化函数	    	            */
/* 说  明					    */
/****************************************************/
void SaveInBlock(int i)
{   
    int op1,op2,op3;
	
    /*指向基本块第一条语句*/
    CodeFile currentCode = baseBlock[i];
    CodeFile formerCode = null;
    CodeFile laterCode = null;

    /* 循环处理基本块中的各条语句*/
    while ((currentCode!=baseBlock[i+1])&&(currentCode!=null))
    {
        CodeFile substiCode = new CodeFile();
	/*进行等价替换*/
	EquaSubsti(currentCode);

	if ((currentCode.codeR.codekind.equals("ADD"))||(currentCode.codeR.codekind.equals("SUB"))||(currentCode.codeR.codekind.equals("MULT"))||(currentCode.codeR.codekind.equals("DIV"))||(currentCode.codeR.codekind.equals("LTC"))||(currentCode.codeR.codekind.equals("EQC"))||(currentCode.codeR.codekind.equals("AADD")))
	{ 
	    /*调用函数Process处理分量1，返回分量1的编码*/
	    op1 = Process(currentCode,1);
	    /*调用函数Process处理分量2，返回分量2的编码*/
	    op2 = Process(currentCode,2);

	    /*查找可用表达式代码表*/
	    FindECC(currentCode.codeR.codekind,op1,op2,substiCode);
	    /*若找到，当前代码可节省*/
	    if (substiCode.codeR.arg3!=null)
	    { 
                /*向临时变量等价表中添加一项*/
		AppendTempEqua(currentCode.codeR.arg3,substiCode.codeR.arg3);
		/*删除当前代码*/
		formerCode = currentCode.former;
		laterCode = currentCode.next;
		if (formerCode!=null)
		    formerCode.next = laterCode;
		if (laterCode!=null)
		    laterCode.former = formerCode;
                if (formerCode!=null) 
		    currentCode = formerCode;
                else 
                    currentCode = laterCode;
	    }				 
	    else  /*没找到，代码不可节省*/
	    { 
	        /*为结果变量构造一个新的编码，填入值编码表*/
		Vnumber++;
		op3 = Vnumber;
		AppendValuNum(currentCode.codeR.arg3,op3);
		/*构造对应的映象码*/
		MirrorCode mirror = GenMirror(op1,op2,op3);
		/*当前代码写入可用表达式代码表*/
	        AppendUsExpr(currentCode,mirror);
	    }
        }
	else if (currentCode.codeR.codekind.equals("ASSIG"))
        {
	    /*Process函数处理赋值右部，返回编码*/
	    op1 = Process(currentCode,1);
				
	    /*若是间接临时变量，op1是地址码；否则，是值码*/
	    op2 = op1;
				
	    /*替换编码表中赋值左部的值编码*/
	    SubstiVcode(currentCode.codeR.arg2,op2);

	    /*删除可用表达式代码表中用到赋值左部值编码的项*/
	    DelUsExpr(currentCode.codeR.arg2);
	}
	/*处理下一条代码*/
	currentCode = currentCode.next;
    }
}
/****************************************************/
/* 函数名  EquaSubsti				    */
/* 功  能  利用临时变量等价表对当前代码进行等价替换 */
/* 说  明  					    */
/****************************************************/
void EquaSubsti(CodeFile code)
{	
    TempEqua Entry = new TempEqua();
    if (code.codeR.arg1!=null)
	/*若操作数1是临时变量,且存在于临时变量等价表中，则替换*/
	if (code.codeR.arg1.form.equals("AddrForm"))
	    if(code.codeR.arg1.midAttr.addr.dataLevel == -1)
	    {	
                FindTempEqua(code.codeR.arg1,Entry);
		if (Entry.arg2!=null)
		    code.codeR.arg1 = Entry.arg2;
	    }
    if (code.codeR.arg2!=null)
	/*若操作数2是临时变量,且存在于临时变量等价表中，则替换*/
	if (code.codeR.arg2.form.equals("AddrForm"))
	    if (code.codeR.arg2.midAttr.addr.dataLevel == -1)
	    {	
                FindTempEqua(code.codeR.arg2,Entry);
		if (Entry.arg2!=null)
		    code.codeR.arg2 = Entry.arg2;
	    }
}
/****************************************************/
/* 函数名  Process				    */
/* 功  能  处理操作分量，并返回对应的编码	    */
/* 说  明  若首次出现，则分配新编码，填入编码表中， */
/*	   返回值取这个新的编码；否则，根据是否间接 */
/*	   变量，返回相应的值编码或地址码	    */	
/****************************************************/
int Process(CodeFile code,int i)
{
    ArgRecord arg;
    ValuNum Entry = new ValuNum();
    String codekind = code.codeR.codekind;
    int opC;
    if (i==1)
	arg = code.codeR.arg1;
    else    
        arg = code.codeR.arg2;
    /*若操作数首次出现，则填入编码表*/
    SearchValuNum(arg,Entry);
    if (Entry.access==null)
    { 
        Vnumber++;
	opC = Vnumber;  /*op1记录操作数1的值编码*/
	AppendValuNum(arg,opC);
    }
    else 
    {  
	/*间接临时变量*/
        if (Entry.access.equals("indir"))
	    /*用间接临时变量的地址码*/
	    if((codekind.equals("AADD"))||(codekind.equals("ASSIG")))
		/*取地址码*/
		opC= Entry.codeInfo.twoCode.addrcode;
	    else   /*否则，取值码*/
		opC = Entry.codeInfo.twoCode.valuecode;
	/*非间接临时变量*/
	else    
            opC = Entry.codeInfo.valueCode;
    }
    return opC;
}
/****************************************************/
/* 函数名  FindTempEqua				    */
/* 功  能  查找临时变量等价表			    */
/* 说  明  					    */
/****************************************************/
void FindTempEqua(ArgRecord arg,TempEqua Entry)
{	 
    TempEqua tItem = tempEquaT;
    while (tItem!=null)
    { /*注：因为是临时变量，故这里可以直接使用引用比较
	而一般变量则不行，因为同一个变量可能会产生
	多个内容相同的ARG结构，根据中间代码生成时的
	处理手段*/
	if (tItem.arg1 == arg)
	    break;
	tItem = tItem.next;
    }
    if (tItem!=null)
    {
        Entry.arg1 = tItem.arg1;
        Entry.arg2 = tItem.arg2;
        Entry.next = tItem.next;
    }    
}
/****************************************************/
/* 函数名  SearchValuNum			    */
/* 功  能  查找编码表				    */
/* 说  明  若存在于编码表中返回入口地址；否则返回空 */
/****************************************************/
void SearchValuNum(ArgRecord arg,ValuNum Entry)
{   
    boolean equal = false;
    /*指向编码表*/
    ValuNum vItem = valuNumT;
    while (vItem != null)
    {   /*比较是否有相同的变量*/  
	equal = IsEqual(vItem.arg,arg);
        if (equal)
	    break;
        vItem = vItem.next;
    }
    if (vItem!=null)
    {
        Entry.arg = vItem.arg;
        Entry.access = vItem.access;
        Entry.codeInfo = vItem.codeInfo;
        Entry.next = vItem.next;
    }
}
/****************************************************/
/* 函数名  IsEqual				    */
/* 功  能  判断两个ARG结构是否相同		    */
/* 说  明  这里运算分量没有标号，故只考虑了常量类   */
/*	   和地址类ARG结构			    */
/****************************************************/
boolean IsEqual(ArgRecord arg1,ArgRecord arg2)
{
    boolean equal = false;
    /*注：应比较ARG结构内容，不能比较引用，因为一个相同的变量
      可能会产生多个相同的ARG结构，由不同的引用，这是
      由中间代码生成时的处理策略决定的*/
    if (arg1.form == arg2.form)
    {
        if (arg1.form.equals("ValueForm"))
	{/*常数类：值相等则等价*/
	    if (arg1.midAttr.value == arg2.midAttr.value)
	        equal = true;
	}
	 /*地址类：层数，偏移，访问方式都相等时等价*/
	else if (arg1.form.equals("AddrForm"))
        { 
	    if ((arg1.midAttr.addr.dataLevel == arg2.midAttr.addr.dataLevel)
	    &&(arg1.midAttr.addr.dataOff == arg2.midAttr.addr.dataOff)
	    &&(arg1.midAttr.addr.access == arg2.midAttr.addr.access))
		equal = true;
	}
    }
    return equal;
}				
/****************************************************/
/* 函数名  AppendValuNum			    */
/* 功  能  当前变量及值写入值编码表中		    */
/* 说  明  申请一个新节点，根据变量是否为间接临时变 */
/*	   填写不同的内容，并联入表中		    */
/****************************************************/
void AppendValuNum(ArgRecord arg,int Vcode)
{   
    /*最后一个节点引用*/
    ValuNum last;
    /*申请一个新的值编码表的节点,并填写内容*/
    ValuNum newItem = new ValuNum();
    newItem.arg = arg;
    /*若是间接临时变量*/
    if ((arg.form.equals("AddrForm"))&&(arg.midAttr.addr.dataLevel == -1)&&(arg.midAttr.addr.access.equals("indir")))
    { 
	newItem.access = "indir";
        newItem.codeInfo.twoCode = new TwoCode();
        newItem.codeInfo.twoCode.valuecode = Vcode;
	newItem.codeInfo.twoCode.addrcode = Vcode;
    }
    else 
    {
        /*其余情况为：非间接临时变量*/		
	newItem.access = "dir";
	newItem.codeInfo.valueCode = Vcode;
    }	
    /*节点联入值编码表中*/
    if (valuNumT == null)
	valuNumT = newItem;
    else 
    {  
        last = valuNumT;
	while (last.next!=null)
	    last = last.next;
	last.next = newItem;
    }
}
/****************************************************/
/* 函数名  AppendTempEqua			    */
/* 功  能  将两个等价的临时变量写入临时变量等价表中 */
/* 说  明  表示第一项可以被第二项替换		    */
/****************************************************/
void AppendTempEqua(ArgRecord arg1,ArgRecord arg2)
{
    /*最后一个节点指针*/
    TempEqua last;
    /*申请一个新的临时变量等价表的节点,并填写内容*/
    TempEqua newItem = new TempEqua();
    newItem.arg1 = arg1;
    newItem.arg2 = arg2;

    /*节点联入临时变量等价表中*/
    if (tempEquaT == null)
	tempEquaT = newItem;
    else 
    {  
        last = tempEquaT;
	while (last.next!=null)
	    last = last.next;
	last.next = newItem;
    }
}
/****************************************************/
/* 函数名  AppendUsExpr				    */
/* 功  能  将中间代码和相应的映象码写可用表达式表中 */
/* 说  明					    */
/****************************************************/	
void AppendUsExpr(CodeFile code,MirrorCode mirror)
{
    UsableExpr last;
    UsableExpr newItem = new UsableExpr();
    newItem.code = code;
    newItem.mirrorC = mirror;

    if (usableExprT==null)
	usableExprT = newItem;
    else 
    { 
        last = usableExprT;
	while (last.next!=null)
	    last = last.next;
	last.next = newItem;
    }
}
/****************************************************/
/* 函数名  FindECC				    */
/* 功  能  判断可用表达式表中是否有可用的表达式代码 */
/* 说  明  若有，返回用于替换的中间代码引用	    */
/*	   否则，返回为空			    */
/****************************************************/
void FindECC(String codekind,int op1Code,int op2Code,CodeFile substiCode)
{   
    UsableExpr currentItem = usableExprT;

    while ((currentItem!=null)&&(substiCode.codeR.arg3==null))
    {   /*语句类别相同*/
	if (currentItem.code.codeR.codekind == codekind)
	/*对应分量编码都相同,可替换*/
	    if ((currentItem.mirrorC.op1==op1Code)&&(currentItem.mirrorC.op2==op2Code))
            {
	        substiCode.codeR = currentItem.code.codeR;
                substiCode.former = currentItem.code.former;
                substiCode.next = currentItem.code.next;
            }
	    else
            {
	        /*可交换运算符，分量编码交叉相同，也可替换*/
	        if ((codekind.equals("ADD"))||(codekind.equals("MULT")))
	            if ((currentItem.mirrorC.op1==op2Code)&&(currentItem.mirrorC.op1==op2Code))
                    {
	        	substiCode.codeR = currentItem.code.codeR;
                	substiCode.former = currentItem.code.former;
                	substiCode.next = currentItem.code.next;
                    }
            }
        currentItem = currentItem.next;
    }
}
/****************************************************/
/* 函数名  GenMirror				    */
/* 功  能  构造映象码				    */
/* 说  明					    */
/****************************************************/
MirrorCode GenMirror(int op1,int op2,int result)
{
    MirrorCode mirror = new MirrorCode();
    mirror.op1 = op1;
    mirror.op2 = op2;
    mirror.result = result;

    return mirror;
}
/****************************************************/
/* 函数名  SubstiVcode				    */
/* 功  能  将当前变量，及其值编码写入编码表	    */
/* 说  明  若变量首次出现，则添加一项；否则，将表中 */
/*	   此变量的值编码替换为新的值编码	    */
/****************************************************/	
void SubstiVcode(ArgRecord arg,int Vcode)
{
    ValuNum Entry = new ValuNum();
    SearchValuNum(arg,Entry);

    /*若操作数首次出现，则填入编码表*/
    if (Entry.access==null)
	AppendValuNum(arg,Vcode);
    else 
    { 
	/*间接临时变量*/
	if (Entry.access.equals("indir"))
	    Entry.codeInfo.twoCode.valuecode = Vcode;			
	/*非间接临时变量*/
	else    
            Entry.codeInfo.valueCode = Vcode;	
    }		
}
/****************************************************/
/* 函数名  DelUsExpr				    */
/* 功  能  将可用表达式代码表中用到arg的值编码的项  */
/*	   删除					    */
/* 说  明					    */
/****************************************************/	
void DelUsExpr(ArgRecord arg)
{
    boolean same = false;
    UsableExpr Item = usableExprT;
    UsableExpr former = Item;  
    while (Item!=null)
    {   /*因为AADD用的是地址码，所以不考虑*/
	if (!(Item.code.codeR.codekind.equals("AADD")))
	{
            if ((Item.code.codeR.arg1 == arg)||(Item.code.codeR.arg2 == arg)||(Item.code.codeR.arg3 == arg))
	        same = true;
	    if (same) 
	    {   /*删除这个可用表达式项*/ 
	        if (Item == usableExprT)
		{ 
                    usableExprT = Item.next;
		    former = usableExprT;
                    Item = usableExprT;
		}
		else 
		{  
                    former.next = Item.next;
		    Item = former.next;
		}
                /*跳到下一次循环开始处*/
		continue;
	    }
	}
        /*指针后移，比较下一个节点*/
	former = Item;
	Item = Item.next;
    }
}

/****************************************************/
/* 函数名  LoopOpti				    */
/* 功  能  循环不变式优化主函数		            */
/* 说  明					    */
/****************************************************/
CodeFile LoopOpti()
{   
    /*从第一条代码开始优化过程*/
    CodeFile currentCode = firstCode;

    /*循环处理每条代码，直到中间代码结束*/
    while (currentCode!=null)
    {   
	if ((currentCode.codeR.codekind.equals("AADD"))||(currentCode.codeR.codekind.equals("ADD"))||(currentCode.codeR.codekind.equals("SUB"))||(currentCode.codeR.codekind.equals("MULT"))||(currentCode.codeR.codekind.equals("DIV"))||(currentCode.codeR.codekind.equals("LTC"))||(currentCode.codeR.codekind.equals("EQC")))	
	    /*将被定值的变量名填入变量定值表中*/
	    AddTable(currentCode.codeR.arg3);
        else if (currentCode.codeR.codekind.equals("ASSIG"))
	    /*将被定值的变量填入变量定值表中*/
	    AddTable(currentCode.codeR.arg2);
        else if (currentCode.codeR.codekind.equals("WHILESTART"))
	    /*循环入口*/
	    whileEntry(currentCode);
        else if (currentCode.codeR.codekind.equals("ENDWHILE"))
	    /*循环出口*/
	    whileEnd(currentCode);
        else if (currentCode.codeR.codekind.equals("CALL"))
	    /*过程调用语句*/
	    call(currentCode);

        /*令指针指向下一条代码*/
	currentCode = currentCode.next;	 
    }
    return firstCode;
}
/****************************************************/
/* 函数名  whileEntry				    */
/* 功  能  循环入口部分的处理函数		    */
/* 说  明					    */
/****************************************************/
void whileEntry(CodeFile code)
{
    LoopInfo infoItem = new LoopInfo();

    /*外提标志初始化为可以外提标志1*/
    infoItem.state = 1;
    /*此循环在变量定值表的入口*/
    infoItem.varDef = TotalNum;
    /*循环入口指针*/
    infoItem.whileEntry = code;
    /*循环出口此处不能确定*/
    infoItem.whileEnd = null;
    /*循环信息表压栈*/
    PushLoop(infoItem);
}
/****************************************************/
/* 函数名  call					    */
/* 功  能  遇到过程调用语句的特别处理		    */
/* 说  明  所有包含此调用语句的循环都不能做不变式   */
/*	   外提					    */
/****************************************************/
void call(CodeFile code)
{ 
    /*所有打开着的循环均为不可外提状态，是这些循环信息中的
      State取0*/
    LoopStack Item = loopTop;
  
    while (Item!=null)
    { 
        Item.loopInfo.state = 0;
        Item = Item.under;
    }
}
/****************************************************/
/* 函数名  whileEnd				    */
/* 功  能  循环出口部分的处理函数		    */
/* 说  明					    */
/****************************************************/
void whileEnd(CodeFile code)
{
    /*循环信息栈的栈顶*/
    LoopStack Item = loopTop;

    /*可以外提*/
    if (Item.loopInfo.state==1)
    {   
	/*填写循环出口位置的引用*/
	loopTop.loopInfo.whileEnd = code;
	/*找到循环入口*/
        CodeFile entry  = loopTop.loopInfo.whileEntry;
        /*循环外提处理部分*/
        LoopOutside(entry);
    }

    /*弹循环信息栈，此层循环处理结束*/
    PopLoop();
}
/****************************************************/
/* 函数名  LoopOutside				    */
/* 功  能  循环外提处理函数			    */
/* 说  明					    */
/****************************************************/
void LoopOutside(CodeFile entry)
{
    /*外提的位置，为循环入口位置*/
    CodeFile place = entry;
    /*当前处理代码,注：跳过循环开始标号语句*/
    CodeFile code =  entry.next;
   
    /*取循环信息栈顶指针*/
    LoopStack Item = loopTop;
    /*取得本层循环的出口位置*/
    CodeFile end = Item.loopInfo.whileEnd;
    /*取得本层循环的变量信息表*/
    int head = Item.loopInfo.varDef;
    int present1, present2;

    /*用于跳过内层循环*/
    int Level = 0;

    /*循环检查每条代码是否可以外提，直到此层循环结束*/
    while (code!=end)
    {   
	if (code.codeR.codekind.equals("WHILESTART"))
	    Level++;	
	else if (code.codeR.codekind.equals("ENDWHILE"))  
	    Level--;
	else if ((code.codeR.codekind.equals("ADD"))||(code.codeR.codekind.equals("SUB"))||(code.codeR.codekind.equals("MULT"))||(code.codeR.codekind.equals("AADD")))	
        {
	    /*跳过内层循环*/
	    if (Level==0)
	    {
		present1 = SearchTable(code.codeR.arg1,head);
		present2 = SearchTable(code.codeR.arg2,head);
		/*两个分量都不在变量定值标号中，可以外提*/
		if ((present1<0)&&(present2<0))
		{  
                    /*操作结果也是不变量，故若在表中，从表中删除*/
		    DelItem(code.codeR.arg3,head);
		    /*外提*/
		    /*在当前位置，删除此代码*/
		    CodeFile formerCode = code.former;
		    CodeFile nextCode = code.next;
		    formerCode.next = nextCode;
		    nextCode.former = formerCode;

		    /*将代码加入到应外提的位置*/
		    CodeFile fplace = place.former;
		    fplace.next  = code;
		    code.former = fplace;
  		    code.next = place;
		    place.former = code;

		    /*回到当前位置处，准备处理下一条语句*/
		    code = formerCode;					
		}
		else
		    /*否则，将变量定值加入当前变量定值表中*/ 
		    AddTable(code.codeR.arg3);
	    }
	}
        /*检查下一条语句*/
	code = code.next;
    }
}
/****************************************************/
/* 函数名  SearchTable				    */
/* 功  能  循环变量定值表查找函数		    */
/* 说  明  参数head指明了本层循环的变量定值在表中的 */
/*         起始位置，arg表示要查找的变量,返回变量   */
/*	   在表中的位置，若不存在返回值为－1	    */
/****************************************************/
int SearchTable(ArgRecord arg,int head)
{
    /*初始化为负数，不再表中*/
    int present = -1 ;

    if (arg.form.equals("AddrForm"))
    {   
	int level = arg.midAttr.addr.dataLevel;
	int off = arg.midAttr.addr.dataOff;
	/*注：临时变量和源变量都可以通过比较层数和偏移看是否存在
	  于表中*/
	for (int i=head;i<TotalNum;i++)
        {
	    if ((varTable[i].midAttr.addr.dataLevel==level)
&&(varTable[i].midAttr.addr.dataOff==off))
	    {	
                present = i;
		break;
	    }
        }
    }   
    return present;
}
/****************************************************/
/* 函数名  DelItem				    */
/* 功  能  删除变量定值表中此项			    */
/* 说  明					    */
/****************************************************/
void DelItem(ArgRecord arg,int head)
{
    /*调用函数查找变量定值表*/
    int present = SearchTable(arg,head);
    /*若在表中，则删除*/
    if (present!=-1)
    {   
        for (int i=present;i<TotalNum;i++)
            varTable[i] =varTable[i+1];
        TotalNum--;
    }
}
/****************************************************/
/* 函数名  AddTable			            */
/* 功  能  将被定值的变量填入变量定值表		    */
/* 说  明					    */
/****************************************************/
void AddTable(ArgRecord arg)
{  
    /*若不在循环中，则从头查表，以免表中重复填入相同的变量*/
    int head = 0;
    /*若在循环中，则只要在当前循环层没有重复定义即可*/
    if (loopTop!=null)
	head = loopTop.loopInfo.varDef;

    int present = SearchTable(arg,head);
    /*表中没有，则添加*/
    if (present==-1)
    {
       varTable[TotalNum] = arg;
       TotalNum = TotalNum+1;
    }
}
/****************************************************/
/* 函数名  PushLoop			            */
/* 功  能  循环信息栈的压栈实现过程		    */
/* 说  明					    */
/****************************************************/
void PushLoop(LoopInfo t)
{
    LoopStack p = new LoopStack();
    p.loopInfo = t;
    p.under=loopTop;
    loopTop = p;
    loopStackEmpty = false;
}
/****************************************************/
/* 函数名  PopLoop			            */
/* 功  能  循环信息栈的弹栈实现过程		    */
/* 说  明					    */
/****************************************************/
LoopInfo PopLoop()
{     
    LoopStack p;
    LoopInfo backpointer;
    p = loopTop;
    backpointer = p.loopInfo;
    loopTop=loopTop.under;
    if (loopTop==null)
	loopStackEmpty = true;

    return backpointer;
}
}

/********************************************************************/
/* 类  名 AnalYuyi	                                            */
/* 功  能 总程序的处理					            */
/* 说  明 建立一个类，处理总程序                                    */
/********************************************************************/
class AnalYuyi
{
/* SCOPESIZE为符号表scope栈的大小*/
int SCOPESIZE = 1000;

/*scope栈*/
SymbTable scope[]=new SymbTable[SCOPESIZE];

/*记录当前层数*/
int  Level=-1;
/*记录当前偏移；*/
int  Off;
/*记录主程序的displayOff*/
int mainOff;
/*记录当前层的displayOff*/
int savedOff;

/*注：域成员的偏移量从0开始。*/
int fieldOff = 0;

/*记录主程序display表的偏移量*/
int StoreNoff;

/*根据目标代码生成需要，initOff应为AR首地址sp到形参变量区的偏移7*/	
int initOff=7;

/*分别指向整型，字符型，bool类型的内部表示*/
TypeIR  intptr = new TypeIR();
TypeIR  charptr = new TypeIR();
TypeIR  boolptr = new TypeIR();

/*错误追踪标志*/
boolean Error=false;
boolean Error1=false;
String yerror;
String serror;
TreeNode yuyiTree;

AnalYuyi(String s)
{
    Recursion r=new Recursion(s);
    Error1=r.Error;
    if (Error1)
        serror=r.serror;
    else
    {
        yuyiTree=r.yufaTree;
        Analyze(yuyiTree);
    }
}
/****************************************************/
/****************************************************/
/****************************************************/
/* 函数名  Analyze				    */
/* 功  能  语义分析主函数        		    */
/* 说  明  从语法树的根节点开始，进行语义分析       */	
/****************************************************/
void Analyze(TreeNode t)	
{ 
    TreeNode p = null;
    TreeNode pp = t;

    /*建立一个新的符号表，开始语义分析*/
    CreatSymbTable();

    /*调用类型内部表示初始化函数*/
    initiate();

    /*语法树的声明节点*/
    p=t.child[1];
    while (p!=null) 
    {
        if(p.nodekind.equals("TypeK") ) 
	    TypeDecPart(p.child[0]); 
        else if(p.nodekind.equals("VarK") )  
	    VarDecPart(p.child[0]);  
        else if(p.nodekind.equals("ProcDecK") )  
            procDecPart(p);       
	else
	    AnalyzeError(t,"no this node kind in syntax tree!",null);
        p = p.sibling ;/*循环处理*/
     }
	
    /*程序体*/
    t = t.child[2];
    if(t.nodekind.equals("StmLK"))
	BodyA(t);
	
    /*撤销符号表*/
    if (Level!=-1)
        DestroySymbTable();
	
    /*输出语义错误*/
    if(Error)
	AnalyzeError(null," Analyze Error ",null);
} 

/****************************************************/
/* 函数名  TypeDecPart				    */
/* 功  能  处理一个类型声明     		    */
/* 说  明  根据语法树中的类型声明节点，取相应内容， */
/*	   将类型标识符添入符号表.	            */
/****************************************************/            
void TypeDecPart(TreeNode t)
{ 
    boolean present=false;
    AttributeIR Attrib=new AttributeIR();  /*存储当前标识符的属性*/
    SymbTable entry = new SymbTable();

    Attrib.kind="typekind";  
    while (t!=null)   
    {
	/*调用记录属性函数，返回是否重复声明错和入口地址*/
	present = Enter(t.name[0],Attrib,entry);	
	
	if (present)
	{
	    AnalyzeError(t," id repeat declaration ",t.name[0]); 
	    entry = null;
	}
	else 
	    entry.attrIR.idtype = TYPEA(t,t.kind);
	t = t.sibling;
    }  
}
/****************************************************/
/* 函数名  TYPEA 				    */
/* 功  能  建立类型的内部表示     		    */
/* 说  明  调用具体类型处理完成类型内部表示的构造   */
/*	   返回指向类型内部表示的指针.	            */
/****************************************************/   
TypeIR TYPEA(TreeNode t,String kind)
{ 
    TypeIR typeptr=null;
	
    /*根据不同类型信息，调用相应的类型处理函数*/
    if (kind.equals("IdK")) 
	typeptr= NameTYPEA(t);
    else if (kind.equals("IntegerK"))  
        typeptr= intptr;                                        
    else if (kind.equals("CharK"))  
	typeptr= charptr;                                      
    else if (kind.equals("ArrayK"))    
        typeptr= ArrayTYPEA(t); 
    else if (kind.equals("RecordK"))  
        typeptr= RecordTYPEA(t); 
    else
    { 
        AnalyzeError(t,"bug: no this type in syntax tree ",null);
        return null;
    }	
    return typeptr;
}
/****************************************************/
/* 函数名  NameTYPEA				    */
/* 功  能  处理类型为类型标识符时的情形	            */
/* 说  明  不构造新的类型，返回此类型标识符的类型， */
/*	   并检查语义错误                           */
/****************************************************/  
TypeIR NameTYPEA(TreeNode t)
{  
    SymbTable Entry=new SymbTable();                     
    TypeIR temp=null;
    boolean present;

    present= FindEntry(t.attr.type_name,Entry);
    /*检查类型标识符未声明错*/
    if (!present)
	AnalyzeError(t," id use before declaration ",t.attr.type_name);
    /*检查非类型标识符错*/  
    else if (!(Entry.attrIR.kind.equals("typekind")))
	AnalyzeError(t," id is not type id ",t.attr.type_name);
    /*返回标识符的类型的内部表示*/
    else
    {  
        temp= Entry.attrIR.idtype;
        return temp;
    }
    return temp;
}	 
/****************************************************/
/* 函数名  ArrayTypeA				    */
/* 功  能  构造数组类型的内部表示   		    */
/* 说  明  处理下标类型，成员类型，计算数组大小，   */
/*	   并检查下标超界错误		            */
/****************************************************/   
TypeIR ArrayTYPEA(TreeNode t)
{ 
    TypeIR tempforchild;

    /*建立一个新的数组类型的内部表示*/
    TypeIR typeptr=new TypeIR();
    typeptr.array=new Array();
    typeptr.kind="arrayTy";
    /*下标类型是整数类型*/
    typeptr.array.indexTy=intptr;                         
    /*成员类型*/
    tempforchild=TYPEA(t,t.attr.arrayAttr.childtype);
    typeptr.array.elementTy=tempforchild;

    /*检查数组下标出界错误*/
    int up=t.attr.arrayAttr.up;
    int low=t.attr.arrayAttr.low;
    if (up < low)
	AnalyzeError(t," array up smaller than under ",null);
    else  /*上下界计入数组类型内部表示中*/
    {       
        typeptr.array.low = low;
        typeptr.array.up = up;
    }
    /*计算数组的大小*/
    typeptr.size= (up-low+1)*(tempforchild.size);
    /*返回数组的内部表示*/
    return typeptr;
}
/****************************************************/
/* 函数名  RecordTYPEA				    */
/* 功  能  构造记录类型的内部表示   		    */
/* 说  明  构造域表，指针存储在记录的内部表示中，   */
/*	   并计算记录的大小                         */  
/****************************************************/
TypeIR RecordTYPEA(TreeNode t)
{ 
    TypeIR Ptr=new TypeIR();  /*新建记录类型的节点*/
    Ptr.body=new FieldChain();
    Ptr.kind="recordTy";
	
    t = t.child[0];                /*从语法数的儿子节点读取域信息*/

    FieldChain Ptr2=null;
    FieldChain Ptr1=null;
    FieldChain body=null;

    while (t != null)				/*循环处理*/
    {
	/*填写ptr2指向的内容节点*
	 *此处循环是处理此种情况int a,b; */
	for(int i=0 ; i < t.idnum ; i++)
	{     
	    /*申请新的域类型单元结构Ptr2*/  
	    Ptr2 = new FieldChain();            
	    if(body == null)
            {
		body = Ptr2; 
                Ptr1 = Ptr2;
	    }
	    /*填写Ptr2的各个成员内容*/
	    Ptr2.id=t.name[i];
	    Ptr2.unitType = TYPEA(t,t.kind);			 
	    
	    /*如果Ptr1!=Ptr2，那么将指针后移*/
	    if(Ptr2 != Ptr1)          
	    {
		/*计算新申请的单元off*/
		Ptr2.off = (Ptr1.off) + (Ptr1.unitType.size);
		Ptr1.next = Ptr2;
		Ptr1 = Ptr2;
	    }
	}
	/*处理完同类型的变量后，取语法树的兄弟节点*/
	t = t.sibling;
    }	
    /*处理记录类型内部结构*/
	
    /*取Ptr2的off为最后整个记录的size*/
    Ptr.size = Ptr2.off + (Ptr2.unitType.size);
    /*将域链链入记录类型的body部分*/   
    Fcopy(Ptr.body,body);   

    return Ptr;
}
/****************************************************/
/* 函数名  VarDecPart				    */
/* 功  能  变量声明序列分析函数    		    */
/* 说  明  处理所有的变量声明			    */
/****************************************************/
void VarDecPart(TreeNode t) 
{  
    varDecList(t);
}   
/****************************************************/
/* 函数名  varDecList 				    */
/* 功  能  处理一个变量声明或形参声明		    */
/* 说  明  处理一个声明节点中声明的所有标识符，	    */	
/*	   将相关信息添入符号表中，若是形参，还要   */
/*	   构造一个参数信息表，各个标识符在符号表的 */
/*	   位置存储在表中，返回参数表的表头指针     */					
/****************************************************/
void varDecList(TreeNode t)
{ 
    boolean present = false;
    SymbTable  Entry=new SymbTable();
    /*纪录变量的属性*/
    AttributeIR Attrib=new AttributeIR();

    while(t!=null)	/*循环过程*/
    {
	Attrib.kind="varkind";  
	for(int i=0;i<(t.idnum);i++)
	{
	    Attrib.idtype=TYPEA(t,t.kind);
			
	    /*判断识值参还是变参acess(dir,indir)*/	
	    if((t.attr.procAttr!=null)&&(t.attr.procAttr.paramt.equals("varparamType")))
	    {
                Attrib.var = new Var();
		Attrib.var.access = "indir";
		Attrib.var.level = Level;
		/*计算形参的偏移*/
				
		Attrib.var.off = Off;
		Off = Off+1;
	    }/*如果是变参，则偏移加1*/
	    else
	    {
                Attrib.var = new Var();
		Attrib.var.access = "dir";
		Attrib.var.level = Level;
		/*计算值参的偏移*/
		if(Attrib.idtype.size!=0)				
		{
		    Attrib.var.off = Off;
		    Off = Off + (Attrib.idtype.size);
		}
	    }/*其他情况均为值参，偏移加变量类型的size*/
			
	    /*登记该变量的属性及名字,并返回其类型内部指针*/
	    present = Enter(t.name[i],Attrib,Entry);	
	    if(present)
	        AnalyzeError(t," id repeat  declaration ",t.name[0]);
	    else
	        t.table[i] = Entry;
	}
	if(t!=null)
	    t = t.sibling;
    }
	
    /*如果是主程序，则记录此时偏移，用于目标代码生成时的displayOff*/
    if(Level==0)
    {
	mainOff = Off;
	/*存储主程序AR的display表的偏移到全局变量*/
	StoreNoff = Off;
    }
    /*如果不是主程序，则记录此时偏移，用于下面填写过程信息表的noff信息*/ 
    else 
	savedOff = Off;
} 
/****************************************************/
/* 函数名  procDecPart				    */
/* 功  能  一个过程声明的语义分析  		    */
/* 说  明  处理过程头，声明，过程体		    */	
/****************************************************/
void procDecPart(TreeNode t)
{ 
    TreeNode p =t;
    SymbTable entry = HeadProcess(t);   /*处理过程头*/
		
    t = t.child[1];
    /*如果过程内部存在声明部分，则处理声明部分*/	
    while (t!=null) 
    {
	if ( t.nodekind.equals("TypeK") ) 
	    TypeDecPart(t.child[0]); 
        else if ( t.nodekind.equals("VarK") )  
            VarDecPart(t.child[0]);  

	/*如果声明部分有函数声明，则跳出循环，先填写noff和moff等信息，*
	*再处理函数声明的循环处理，否则无法保存noff和moff的值。      */
	else if ( t.nodekind.equals("ProcDecK") )  {}
	else
	    AnalyzeError(t,"no this node kind in syntax tree!",null);
				
	if(t.nodekind.equals("ProcDecK"))
            break;
	else
            t=t.sibling ;
    }
    entry.attrIR.proc.nOff = savedOff;
    entry.attrIR.proc.mOff = entry.attrIR.proc.nOff + entry.attrIR.proc.level+1;
    /*过程活动记录的长度等于nOff加上display表的长度*
    *diplay表的长度等于过程所在层数加一           */

    /*处理程序的声明部分*/
    while(t!=null)
    {
	procDecPart(t);
	t = t.sibling;
    }
    t = p;
    BodyA(t.child[2]);/*处理Block*/

    /*函数部分结束，删除进入形参时，新建立的符号表*/
    if ( Level!=-1)
	DestroySymbTable();/*结束当前scope*/
}
/****************************************************/
/* 函数名  HeadProcess				    */
/* 功  能  形参处理函数         		    */
/* 说  明  循环处理各个节点，并将处理每个节点得到   */
/*	   的参数表连接起来，组成整个形参链表，返回 */
/*         这个表的指针				    */	
/****************************************************/
SymbTable HeadProcess(TreeNode t)
{ 
    AttributeIR attrIr = new AttributeIR();
    boolean present = false;
    SymbTable entry = new SymbTable();
		
    /*填属性*/
    attrIr.kind = "prockind";
    attrIr.idtype = null; 
    attrIr.proc = new Proc();
    attrIr.proc.param = new ParamTable();
    attrIr.proc.level = Level+1;	
	
    if(t!=null)
    {
	/*登记函数的符号表项*/		
	present = Enter(t.name[0],attrIr,entry);
	t.table[0] = entry;
	/*处理形参声明表*/
    }
    entry.attrIR.proc.param = ParaDecList(t);

    return entry;
}   
/****************************************************/
/* 函数名  ParaDecList				    */
/* 功  能  处理一个形参节点        		    */
/* 说  明  根据参数是形参还是变参，分别调用变量声明 */
/*	   节点的处理函数，另一个实参是Add，表示处理*/
/*	   的是函数的形参。			    */	
/****************************************************/
ParamTable ParaDecList(TreeNode t)
{ 
    TreeNode p=null;
    ParamTable Ptr1=null; 
    ParamTable Ptr2=null;
    ParamTable head=null;
	
    if(t!=null)
    {
	if(t.child[0]!=null)
	    p = t.child[0];   	/*程序声明节点的第一个儿子节点*/
	
	CreatSymbTable();		/*进入新的局部化区*/
	Off = 7;                /*子程序中的变量初始偏移设为8*/

	VarDecPart(p);			/*变量声明部分*/

	SymbTable Ptr0 = scope[Level];      		 
                                    
	while(Ptr0 != null)         /*只要不为空，就访问其兄弟节点*/
	{
	    /*构造形参符号表，并使其连接至符号表的param项*/
	    Ptr2 = new ParamTable();
	    if(head == null)
            {
		head = Ptr2;
                Ptr1 = Ptr2;
            }
	    //Ptr0.attrIR.var.isParam = true;
	    copy(Ptr2.entry,Ptr0);
			
	    if(Ptr2 != Ptr1)          
	    {
		Ptr1.next = Ptr2;
		Ptr1 = Ptr2;
	    }
	    Ptr0 = Ptr0.next;
	}
    }
    return head;   /*返回形参符号表的头指针*/
}
/****************************************************/
/* 函数名  BodyA				    */
/* 功  能  语句序列处理函数        		    */
/* 说  明  用于处理过程体或者程序体，		    */
/*	  循环处理各个语句			    */	
/****************************************************/
void BodyA(TreeNode t)
{  
    /*令指针指向第一条语句*/
    if (t.nodekind.equals("StmLK"))
	t=t.child[0];

    /*处理语句序列*/
    while (t!=null)
    { 
        /*调用语句处理函数*/
	StatementA(t);
        t= t.sibling;
    }
}
/****************************************************/
/* 函数名  StatementA				    */
/* 功  能  语句处理函数	        		    */
/* 说  明  根据语句的具体类型，分别调用相应的       */
/*	   语句处理函数				    */
/****************************************************/
void StatementA(TreeNode t) 
{  
    if (t.kind.equals("AssignK"))
	AssignSA(t);  
    else if (t.kind.equals("CallK"))      
        CallSA(t);   
    else if (t.kind.equals("ReadK"))     
        ReadSA(t);    
    else if (t.kind.equals("WriteK"))     
        WriteSA(t);	 
    else if (t.kind.equals("IfK"))     
        IfSA(t);	  
    else if (t.kind.equals("WhileK"))	
        WhileSA(t);	  
    else if (t.kind.equals("ReturnK")) 	
        ReturnSA(t);  
    else
        AnalyzeError(t," bug:no this statement in syntax tree ",null);	
}
/****************************************************/
/* 函数名  AssignSA				    */
/* 功  能  赋值语句处理函数	       		    */
/* 说  明  检查左部标识符，调用表达式处理函数，	    */	
/*	   并检查标识符未声明错，非期望标识符错，   */
/*	   赋值不兼容错				    */
/****************************************************/
void AssignSA(TreeNode t)
{ 
    SymbTable entry = new SymbTable();
	
    boolean present = false;
    TypeIR ptr = null;
    TypeIR Eptr = null;
	
    TreeNode child1;
    TreeNode child2;

    child1 = t.child[0];
    child2 = t.child[1];

    if(child1.child[0]==null)
    {	
	/*在符号表中查找此标识符*/
	present = FindEntry(child1.name[0],entry);
		
	if(present)
	{   /*id不是变量*/
            if (!(entry.attrIR.kind.equals("varkind")))
	    {				
                AnalyzeError(t," left and right is not compatible in assign ",null);				                      Eptr = null;
	    }
	    else
            {
	        Eptr = entry.attrIR.idtype;
		child1.table[0] = entry;
            }
	} 
	else /*标识符无声明*/
	    AnalyzeError(t,"is not declarations!",child1.name[0]);
	}
	else/*Var0[E]的情形*/
	{	
            if(child1.attr.expAttr.varkind.equals("ArrayMembV"))
		Eptr = arrayVar(child1);	
	    else /*Var0.id的情形*/
		if(child1.attr.expAttr.varkind.equals("FieldMembV"))
		    Eptr = recordVar(child1);
	}
	if(Eptr != null)
	{	
	    if((t.nodekind.equals("StmtK"))&&(t.kind.equals("AssignK")))
	    {
		/*检查是不是赋值号两侧 类型等价*/
		ptr = Expr(child2,null);
		if (!Compat(ptr,Eptr)) 
		    AnalyzeError(t,"ass_expression error!",child2.name[0]);
	    }
	    /*赋值语句中不能出现函数调用*/
	}
}
/***********************************************************/
/* 函数名 Compat                                           */
/* 功  能 判断类型是否相容                                 */
/* 说  明 由于TINY语言中只有整数类型、字符类型、数组类型和 */
/*        记录类型，故类型相容等于类型等价，只需判断每个结 */
/*        构类型的内部表示产生的指针值是否相同即可。       */
/***********************************************************/
boolean Compat(TypeIR tp1,TypeIR tp2)
{
    boolean  present; 
    if (tp1!=tp2)
	present = false;  /*类型不等*/
    else
	present = true;   /*类型等价*/
    return present;
}

/************************************************************/
/* 函数名  Expr                                             */
/* 功  能  该函数处理表达式的分析                           */
/* 说  明  表达式语义分析的重点是检查运算分量的类型相容性， */
/*         求表达式的类型。其中参数Ekind用来表示实参是变参  */
/*         还是值参。    	                            */
/************************************************************/
TypeIR Expr(TreeNode t,String Ekind)
{
    boolean present = false;
    SymbTable entry = new SymbTable();

    TypeIR Eptr0=null;
    TypeIR Eptr1=null;
    TypeIR Eptr = null;
    if(t!=null)
    {
        if(t.kind.equals("ConstK"))
        { 
	    Eptr = intptr;
	    Eptr.kind = "intTy";
	    if(Ekind!=null)
	        Ekind = "dir";   /*直接变量*/ 
        }
        else if(t.kind.equals("VariK"))
        {
	    /*Var = id的情形*/
	    if(t.child[0]==null)
	    {	
		/*在符号表中查找此标识符*/
		present = FindEntry(t.name[0],entry);				
		t.table[0] = entry;

		if(present)
		{   /*id不是变量*/
		    if (!(entry.attrIR.kind.equals("varkind")))
		    {
			AnalyzeError(t," syntax bug: no this kind of exp ",t.name[0]);				                              Eptr = null;
		    }
		    else
		    {
			Eptr = entry.attrIR.idtype;	
			if (Ekind!=null)
			    Ekind = "indir";  /*间接变量*/						
		    }
		} 
		else /*标识符无声明*/
		    AnalyzeError(t,"is not declarations!",t.name[0]);				
	    }
	    else/*Var = Var0[E]的情形*/
	    {	
                if(t.attr.expAttr.varkind.equals("ArrayMembV"))
		    Eptr = arrayVar(t);	
		/*Var = Var0.id的情形*/
		else if(t.attr.expAttr.varkind.equals("FieldMembV"))
		    Eptr = recordVar(t);
	    }
	}
        else if(t.kind.equals("OpK"))
        {
	    /*递归调用儿子节点*/
	    Eptr0 = Expr(t.child[0],null);
	    if(Eptr0==null)
	        return null;
	    Eptr1 = Expr(t.child[1],null);
	    if(Eptr1==null)
		return null;
							
	    /*类型判别*/
	    present = Compat(Eptr0,Eptr1);
	    if (present)
	    {
		if((t.attr.expAttr.op.equals("LT"))||(t.attr.expAttr.op.equals("EQ")))
		    Eptr = boolptr;
                else if((t.attr.expAttr.op.equals("PLUS"))||(t.attr.expAttr.op.equals("MINUS"))||(t.attr.expAttr.op.equals("TIMES"))||(t.attr.expAttr.op.equals("OVER")))  
		    Eptr = intptr;
                                /*算数表达式*/
		if(Ekind != null)
	            Ekind = "dir"; /*直接变量*/
	    }
	    else 
		AnalyzeError(t,"operator is not compat!",null);
	}
    }
    return Eptr;
}			

/************************************************************/
/* 函数名  arrayVar                                         */
/* 功  能  该函数处理数组变量的下标分析                     */
/* 说  明  检查var := var0[E]中var0是不是数组类型变量，E是不*/
/*         是和数组的下标变量类型匹配。                     */
/************************************************************/
TypeIR arrayVar(TreeNode t)
{
    boolean present = false;
    SymbTable entry = new SymbTable();

    TypeIR Eptr0=null;
    TypeIR Eptr1=null;
    TypeIR Eptr = null;
	
	
    /*在符号表中查找此标识符*/

    present = FindEntry(t.name[0],entry);				
    t.table[0] = entry;	
    /*找到*/
    if(present)
    {
	/*Var0不是变量*/
	if (!(entry.attrIR.kind.equals("varkind")))
	{
	    AnalyzeError(t,"is not variable error!",t.name[0]);			
	    Eptr = null;
	}
	/*Var0不是数组类型变量*/
	else if(entry.attrIR.idtype!=null)
        {
	    if(!(entry.attrIR.idtype.kind.equals("arrayTy")))
	    {
		AnalyzeError(t,"is not array variable error !",t.name[0]);
		Eptr = null;
	    }
	    else
	    {	
		/*检查E的类型是否与下标类型相符*/
		Eptr0 = entry.attrIR.idtype.array.indexTy;
		if(Eptr0==null)
		    return null;
		Eptr1 = Expr(t.child[0],null);//intPtr;
		if(Eptr1==null)
		    return null;
		present = Compat(Eptr0,Eptr1);
		if(!present)
		{
		    AnalyzeError(t,"type is not matched with the array member error !",null);
		    Eptr = null;
		}
		else
		    Eptr = entry.attrIR.idtype.array.elementTy;
	    }
        }
    }
    else/*标识符无声明*/
	AnalyzeError(t,"is not declarations!",t.name[0]);
    return Eptr;
}

/************************************************************/
/* 函数名  recordVar                                        */
/* 功  能  该函数处理记录变量中域的分析                     */
/* 说  明  检查var:=var0.id中的var0是不是记录类型变量，id是 */
/*         不是该记录类型中的域成员。                       */
/************************************************************/
TypeIR recordVar(TreeNode t)
{
	boolean present = false;
	boolean result = false;
	SymbTable entry = new SymbTable();

	TypeIR Eptr0=null;
	TypeIR Eptr1=null;
	TypeIR Eptr = null;
	FieldChain currentP = new FieldChain();
	
	
	/*在符号表中查找此标识符*/
	present = FindEntry(t.name[0],entry);				
	t.table[0] = entry;	
	/*找到*/
	if(present)
	{
	    /*Var0不是变量*/
	    if (!(entry.attrIR.kind.equals("varkind")))
	    {
		AnalyzeError(t,"is not variable error!",t.name[0]);				
		Eptr = null;
	    }
	    /*Var0不是记录类型变量*/
	    else if(!(entry.attrIR.idtype.kind.equals("recordTy")))
	    {
		AnalyzeError(t,"is not record variable error!",t.name[0]);
		Eptr = null;
	    }
	    else/*检查id是否是合法域名*/
	    {
		Eptr0 = entry.attrIR.idtype;
		currentP = Eptr0.body;
		while((currentP!=null)&&(!result))
		{  
        	    result = t.child[0].name[0].equals(currentP.id);
		    /*如果相等*/
		    if(result)
			Eptr = currentP.unitType;
		    else
			currentP = currentP.next;
		}	 
		if(currentP==null)
		    if(!result)
		    {
		        AnalyzeError(t,"is not field type!",t.child[0].name[0]);    				                        Eptr = null;
		    }
	            /*如果id是数组变量*/
		    else if(t.child[0].child[0]!=null)
			Eptr = arrayVar(t.child[0]);
	    }
	}
	else/*标识符无声明*/
	    AnalyzeError(t,"is not declarations!",t.name[0]);
	return Eptr;
}
		
/****************************************************/
/* 函数名  CallSA				    */
/* 功  能  函数调用语句处理函数    		    */
/* 说  明  检查非函数标识符错，调用检查形实参是否   */	
/*		   相容函数			    */
/****************************************************/
void CallSA(TreeNode t)
{ 
	String Ekind=" ";
	boolean present = false;
	SymbTable entry=new SymbTable();
	TreeNode p = null;

	/*用id检查整个符号表*/
	present = FindEntry(t.child[0].name[0],entry);		
        t.child[0].table[0] = entry;

	/*未查到表示函数无声明*/
	if (!present)                     
	    AnalyzeError(t,"function is not declarationed!",t.child[0].name[0]);  
        else 
	    /*id不是函数名*/
	    if (!(entry.attrIR.kind.equals("prockind")))     
		AnalyzeError(t,"is not function name!",t.child[0].name[0]);
	    else/*形实参匹配*/
	    {
		p = t.child[1];
		/*paramP指向形参符号表的表头*/
		ParamTable paramP = entry.attrIR.proc.param;	
		while((p!=null)&&(paramP!=null))
		{
		    SymbTable paraEntry = paramP.entry;
		    TypeIR Etp = Expr(p,Ekind);/*实参*/
		    /*参数类别不匹配*/
		    if ((paraEntry.attrIR.var.access.equals("indir"))&&(Ekind.equals("dir")))
			AnalyzeError(t,"param kind is not match!",null);  
			/*参数类型不匹配*/
                    else if((paraEntry.attrIR.idtype)!=Etp)
			AnalyzeError(t,"param type is not match!",null);
		    p = p.sibling;
		    paramP = paramP.next;
		}
		/*参数个数不匹配*/
		if ((p!=null)||(paramP!=null))
		    AnalyzeError(t,"param num is not match!",null); 
	    }
}
/****************************************************/
/* 函数名  ReadSA				    */
/* 功  能  读语句处理函数	    		    */
/* 说  明  检查标识符未声明错，非变量标识符错	    */
/****************************************************/
void ReadSA(TreeNode t)
{ 
    SymbTable Entry=new SymbTable();
    boolean present=false;
    /*查找变量标识符*/
    present = FindEntry(t.name[0],Entry);
    /*变量在符号表中的地址写入语法树*/
    t.table[0] = Entry;

    if (!present)   /*检查标识符未声明错*/
	AnalyzeError(t," id no declaration in read ",t.name[0]);
    else if (!(Entry.attrIR.kind.equals("varkind")))   /*检查非变量标识符错*/ 
        AnalyzeError(t," not var id in read statement ", null);
}
/****************************************************/
/* 函数名  WriteSA				    */
/* 功  能  写语句处理函数	    		    */
/* 说  明  调用表达式处理函数，检查语义错误	    */
/****************************************************/
void WriteSA(TreeNode t)  
{ 
    TypeIR Etp = Expr(t.child[0],null);	
    if(Etp!=null)
	/*如果表达式类型为bool类型，报错*/
	if (Etp.kind.equals("boolTy"))
		AnalyzeError(t,"exprssion type error!",null);
}
/****************************************************/
/* 函数名  IfSA					    */
/* 功  能  条件语句处理函数	    		    */
/* 说  明  检查非布尔表达式错，并调用语句序列函数   */
/*	   处理then部分和 else 部分	            */	
/****************************************************/
void IfSA(TreeNode t)
{ 
    String Ekind=null;
    TypeIR Etp;
    Etp=Expr(t.child[0],Ekind);
    
    if (Etp!=null)   /*表达式没有错误*/
        if (!(Etp.kind.equals("boolTy")))   /*检查非布尔表达式错*/
	    AnalyzeError(t," not bool expression in if statement ",null);
	else
	{
	    TreeNode p = t.child[1];
	    /*处理then语句序列部分*/
	    while(p!=null)
	    {
		StatementA(p);
		p=p.sibling;
	    }
	    t = t.child[2];		/*必有三儿子*/
	    /*处理else语句部分*/
	    while(t!=null)
	    {
		StatementA(t);	
		t=t.sibling;
	    }
	}
}
/****************************************************/
/* 函数名  WhileSA				    */
/* 功  能  循环语句处理函数	    		    */
/* 说  明  检查非布尔表达式错，并调用语句序列函数   */
/*	   处理循环体			            */	
/****************************************************/
void  WhileSA(TreeNode t)
{ 
    TypeIR Etp;
    Etp=Expr(t.child[0],null);
   
    if (Etp!=null)  /*表达式没有错*/	  
        if (!(Etp.kind.equals("boolTy")))   /*检查非布尔表达式错*/
	    AnalyzeError(t," not bool expression in if statement ",null);
    /*处理循环体*/
    else
    {
	t = t.child[1];
	/*处理循环部分*/
	while(t!=null)
	{ 
	    StatementA(t);
	    t=t.sibling;
	}
    }
}
/****************************************************/
/* 函数名  ReturnSA				    */
/* 功  能  返回语句处理函数	    		    */
/* 说  明  若出现在主程序中，则语义错误		    */	
/****************************************************/
void  ReturnSA(TreeNode t)
{
    if (Level == 0)
	AnalyzeError(t," return statement cannot in main program ",null);
}

/****************************************************/
/*****************功能函数***************************/
/****************************************************/
/* 函数名  AnalyzeError				    */
/* 功  能  给出语义错误提示信息			    */
/* 说  明  Error设置为true,防止错误的传递	    */
/****************************************************/
void AnalyzeError(TreeNode t,String message,String s)
{   
    if (t==null)
        yerror=yerror+"\n>>> ERROR:"+"Analyze error "+":"+message+s+"\n"; 
    else
        yerror=yerror+"\n>>> ERROR :"+"Analyze error at "+String.valueOf(t.lineno)+": "+message+s+"\n";  

    /* 设置错误追踪标志Error为TRUE,防止错误进一步传递 */
    Error = true;
}
/****************************************************/
/* 函数名  initiate				    */
/* 功  能  建立整型，字符型，布尔类型的内部表示	    */
/* 说  明  这几个类型的内部表示式固定的，只需建立   */
/*	   一次，以后引用相应的引用即可	            */	
/****************************************************/
void initiate()
{   
    /*整数类型的内部表示*/
    intptr.kind="intTy";
    intptr.size=1;
    /*字符类型的内部表示*/
    charptr.kind="charTy";
    charptr.size=1;
    /*布尔类型的内部表示*/
    boolptr.kind="boolTy";
    boolptr.size=1;
}

/********************************************************/
/*************符号表相关函数*****************************/
/********************************************************/
/* 函数名  CreatSymbTable			        */
/* 功  能  创建一个符号表				*/
/* 说  明  并没有真正生成新的符号表，只是层数加一	*/
/********************************************************/
void  CreatSymbTable()
{ 	
    Level = Level +1; 
    scope[Level] = null;	
    Off = initOff;  /* 根据目标代码生成需要，initOff应为AR首地址sp
		       到形参变量区的偏移7 */	
}
/********************************************************/
/* 函数名  DestroySymbTable				*/
/* 功  能  删除一个符号表				*/
/* 说  明  并不真正释放这个符号表空间，只是改变scope栈  */
/********************************************************/
void  DestroySymbTable()
{
    /*用层数减1，来表示删除当前符号表*/
    Level = Level - 1;
}
/**********************************************************/
/* 函数名  Enter					  */
/* 功  能  将一个标识符及其属性登记到符号表		  */
/* 说  明  返回值决定标识符是否重复，由Entry带回此标识符  */
/*         在符号表中的位置，若重复，则不登记，Entry为找  */
/*	   到的那个标识符的位置				  */
/**********************************************************/
boolean Enter(String id,AttributeIR attribP,SymbTable entry)
{ 
    boolean present = false;
    boolean result = false;
    SymbTable curentry=null;
    SymbTable prentry=null;

    if(scope[Level]==null)
    {
	scope[Level] = new SymbTable();
	curentry = scope[Level];
    }
    else
    {
        curentry = scope[Level];
	while (curentry != null)
	{
	    prentry = curentry;
	    result = id.equals(curentry.idName);
	    if(result)
	    {
		AnalyzeError(null," Enter , id repeat declaration ",null);
		present = true;
	    }
	    else
		curentry = prentry.next;
	}   /*在该层符号表内检查是否有重复定义错误*/
    
	if(!present)
	{
	    prentry.next = new SymbTable();
	    curentry = prentry.next;
	}
    }
		
    /*将标识符名和属性登记到表中*/
    curentry.idName = id;

    curentry.attrIR.idtype = attribP.idtype;
    curentry.attrIR.kind = attribP.kind;
    if (attribP.kind.equals("typekind"))
	{}
    else if (attribP.kind.equals("varkind")) 
    {
        curentry.attrIR.var=new Var();
	curentry.attrIR.var.level=attribP.var.level;
	curentry.attrIR.var.off=attribP.var.off;
	curentry.attrIR.var.access=attribP.var.access;
    }
    else if (attribP.kind.equals("prockind")) 
    {
        curentry.attrIR.proc=new Proc();
	curentry.attrIR.proc.level=attribP.proc.level;
	curentry.attrIR.proc.param=attribP.proc.param;
    }
    copy(entry,curentry);
	
    return present;
}
/********************************************************/
/* 函数名  FindEntry    				*/
/* 功  能  查找一个标识符是否在符号表中			*/
/* 说  明  根据flag决定是查找当前符号表，还是所有符号表 */
/*	   返回值决定是否找到，变量Entry返回此标识符在  */
/*	   符号表中的位置				*/
/********************************************************/
boolean FindEntry(String id,SymbTable entry)
{ 
	boolean  present=false;  /*返回值*/
	boolean result = false;         /*标识符名字比较结果*/
	int lev = Level;	 /*临时记录层数的变量*/

	SymbTable findentry = scope[lev];

	while((lev!=-1)&&(!present))
	{
	    while ((findentry!=null)&&(!present))
	    {
		result = id.equals(findentry.idName);
		if (result)
		    present = true;    
		/*如果标识符名字相同，则返回TRUE*/
		else 
		    findentry = findentry.next;
		/*如果没找到，则继续链表中的查找*/
	    }
	    if(!present)
	    {
		lev = lev-1;
                if(lev != -1)
		    findentry = scope[lev];			
	    }
	}/*如果在本层中没有查到，则转到上一个局部化区域中继续查找*/
        if (!present)
	    entry = null;
	else 
	    copy(entry,findentry);

	return present;
}
/********************************************************/
/* 函数名  copy	  				        */
/* 功  能  复制函数    				        */
/* 说  明  将b中的内容复制给a			        */
/********************************************************/
void copy(SymbTable a,SymbTable b)
{
    a.idName=b.idName;
    a.attrIR=b.attrIR;
    a.next=b.next;
}
/********************************************************/
/* 函数名  Fcopy	  				*/
/* 功  能  复制函数    				        */
/* 说  明  将b中的内容复制给a			        */
/********************************************************/
void Fcopy(FieldChain a,FieldChain b)
{
    a.id=b.id;
    a.off=b.off;
    a.unitType=b.unitType;
    a.next=b.next;
}

}
/********************************************************************/
/************************语 法 分 析*********************************/
/********************************************************************/
/********************************************************************/
/* 类  名 Recursion	                                            */
/* 功  能 总程序的处理					            */
/* 说  明 建立一个类，处理总程序                                    */
/********************************************************************/
class Recursion
{       
TokenType token=new TokenType();

int MAXTOKENLEN=10;
int  lineno=0;
String temp_name;
StringTokenizer fenxi;

boolean Error=false;
String serror;
TreeNode yufaTree;

Recursion(String s)
{
    yufaTree=Program(s);
}

/********************************************************************/
/********************************************************************/
/********************************************************************/
/* 函数名 Program					            */
/* 功  能 总程序的处理函数					    */
/* 产生式 < Program > ::= ProgramHead DeclarePart ProgramBody .     */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/*        语法树的根节点的第一个子节点指向程序头部分ProgramHead,    */
/*        DeclaraPart为ProgramHead的兄弟节点,程序体部分ProgramBody  */
/*        为DeclarePart的兄弟节点.                                  */
/********************************************************************/
TreeNode Program(String ss)
{
        fenxi=new StringTokenizer(ss,"\n");
        ReadNextToken();

        TreeNode root = newNode("ProcK");
	TreeNode t=ProgramHead();
	TreeNode q=DeclarePart();
	TreeNode s=ProgramBody();		
       
	if (t!=null) 
            root.child[0] = t;
	else 
            syntaxError("a program head is expected!");
	if (q!=null) 
            root.child[1] = q;
	if (s!=null) 
            root.child[2] = s;
	else syntaxError("a program body is expected!");

	match("DOT");
        if (!(token.Lex.equals("ENDFILE")))
	    syntaxError("Code ends before file\n");

	return root;
}

/**************************函数头部分********************************/
/********************************************************************/
/********************************************************************/
/* 函数名 ProgramHead						    */
/* 功  能 程序头的处理函数					    */
/* 产生式 < ProgramHead > ::= PROGRAM  ProgramName                  */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode ProgramHead()
{
    TreeNode t = newNode("PheadK");
    match("PROGRAM");
    if (token.Lex.equals("ID"))
        t.name[0]=token.Sem;
    match("ID");
    return t;
}	
    
/**************************声明部分**********************************/
/********************************************************************/	
/********************************************************************/
/* 函数名 DeclarePart						    */
/* 功  能 声明部分的处理					    */
/* 产生式 < DeclarePart > ::= TypeDec  VarDec  ProcDec              */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode DeclarePart()
{
    /*类型*/
    TreeNode typeP = newNode("TypeK");    	 
    TreeNode tp1 = TypeDec();
    if (tp1!=null)
        typeP.child[0] = tp1;
    else
	typeP=null;

    /*变量*/
    TreeNode varP = newNode("VarK");
    TreeNode tp2 = VarDec();
    if (tp2 != null)
        varP.child[0] = tp2;
    else 
        varP=null;
		 
    /*函数*/
    TreeNode procP = ProcDec();
    if (procP==null)  {}
    if (varP==null)   { varP=procP; }	 
    if (typeP==null)  { typeP=varP; }
    if (typeP!=varP)
	typeP.sibling = varP;
    if (varP!=procP)
        varP.sibling = procP;
    return typeP;
}

/**************************类型声明部分******************************/
/********************************************************************/
/* 函数名 TypeDec					            */
/* 功  能 类型声明部分的处理    				    */
/* 产生式 < TypeDec > ::= ε | TypeDeclaration                      */
/* 说  明 根据文法产生式,调用相应的递归处理函数,生成语法树节点      */
/********************************************************************/
TreeNode TypeDec()
{
    TreeNode t = null;
    if (token.Lex.equals("TYPE"))
        t = TypeDeclaration();
    else if ((token.Lex.equals("VAR"))||(token.Lex.equals("PROCEDURE"))      
            ||(token.Lex.equals("BEGIN"))) {}
         else      
	     ReadNextToken();
    return t;
}

/********************************************************************/
/* 函数名 TypeDeclaration					    */
/* 功  能 类型声明部分的处理函数				    */
/* 产生式 < TypeDeclaration > ::= TYPE  TypeDecList                 */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode TypeDeclaration()
{
    match("TYPE");
    TreeNode t = TypeDecList();
    if (t==null)
        syntaxError("a type declaration is expected!");
    return t;
}

/********************************************************************/
/* 函数名 TypeDecList		 				    */
/* 功  能 类型声明部分的处理函数				    */
/* 产生式 < TypeDecList > ::= TypeId = TypeName ; TypeDecMore       */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode TypeDecList()
{
    TreeNode t = newNode("DecK");
    if (t != null)
    {
	TypeId(t);                               
	match("EQ");  
	TypeName(t); 
	match("SEMI");                           
        TreeNode p = TypeDecMore();
	if (p!=null)
	    t.sibling = p;
    }
    return t;
}

/********************************************************************/
/* 函数名 TypeDecMore		 				    */
/* 功  能 类型声明部分的处理函数				    */
/* 产生式 < TypeDecMore > ::=    ε | TypeDecList                   */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode TypeDecMore()
{
    TreeNode t=null;
    if (token.Lex.equals("ID"))
        t = TypeDecList();
    else if ((token.Lex.equals("VAR"))||(token.Lex.equals("PROCEDURE"))                       ||(token.Lex.equals("BEGIN"))) {}       
         else
	     ReadNextToken();
    return t;
}

/********************************************************************/
/* 函数名 TypeId		 				    */
/* 功  能 类型声明部分的处理函数				    */
/* 产生式 < TypeId > ::= id                                         */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
void TypeId(TreeNode t)
{
    if ((token.Lex.equals("ID"))&&(t!=null))
    {
	t.name[(t.idnum)]=token.Sem;
	t.idnum = t.idnum+1;
    }
    match("ID");
}

/********************************************************************/
/* 函数名 TypeName		 				    */
/* 功  能 类型声明部分的处理				            */
/* 产生式 < TypeName > ::= BaseType | StructureType | id            */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
void TypeName(TreeNode t)
{
   if (t !=null)
   {
      if ((token.Lex.equals("INTEGER"))||(token.Lex.equals("CHAR")))    
          BaseType(t);
      else if ((token.Lex.equals("ARRAY"))||(token.Lex.equals("RECORD")))   
              StructureType(t);
      else if (token.Lex.equals("ID")) 
           {
                 t.kind = "IdK";
	         t.attr.type_name = token.Sem;    
                 match("ID");  
           }
	   else
	       ReadNextToken();
   }
}
/********************************************************************/
/* 函数名 BaseType		 				    */
/* 功  能 类型声明部分的处理函数				    */
/* 产生式 < BaseType > ::=  INTEGER | CHAR                          */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
void BaseType(TreeNode t)
{
       if (token.Lex.equals("INTEGER"))
       { 
             match("INTEGER");
             t.kind = "IntegerK";
       }
       else if (token.Lex.equals("CHAR"))     
             {
                 match("CHAR");
                 t.kind = "CharK";
             }
             else
                ReadNextToken();   
}

/********************************************************************/
/* 函数名 StructureType		 				    */
/* 功  能 类型声明部分的处理函数				    */
/* 产生式 < StructureType > ::=  ArrayType | RecType                */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
void StructureType(TreeNode t)
{
       if (token.Lex.equals("ARRAY"))
       {
           ArrayType(t); 
       }          
       else if (token.Lex.equals("RECORD"))     
            {
                 t.kind = "RecordK";
                 RecType(t);
            }
            else
                ReadNextToken();   
}
/********************************************************************/
/* 函数名 ArrayType                                                 */
/* 功  能 类型声明部分的处理函数			            */
/* 产生式 < ArrayType > ::=  ARRAY [low..top] OF BaseType           */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
void ArrayType(TreeNode t)
{
     t.attr.arrayAttr = new ArrayAttr();
     match("ARRAY");
     match("LMIDPAREN");
     if (token.Lex.equals("INTC"))
	 t.attr.arrayAttr.low = Integer.parseInt(token.Sem);
     match("INTC");
     match("UNDERANGE");
     if (token.Lex.equals("INTC"))
	 t.attr.arrayAttr.up = Integer.parseInt(token.Sem);
     match("INTC");
     match("RMIDPAREN");
     match("OF");
     BaseType(t);
     t.attr.arrayAttr.childtype = t.kind;
     t.kind = "ArrayK";
}

/********************************************************************/
/* 函数名 RecType		 				    */
/* 功  能 类型声明部分的处理函数				    */
/* 产生式 < RecType > ::=  RECORD FieldDecList END                  */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
void RecType(TreeNode t)
{
    TreeNode p = null;
    match("RECORD");
    p = FieldDecList();
    if (p!=null)
        t.child[0] = p;
    else
        syntaxError("a record body is requested!");         
    match("END");
}
/********************************************************************/
/* 函数名 FieldDecList		 				    */
/* 功  能 类型声明部分的处理函数				    */
/* 产生式 < FieldDecList > ::=   BaseType IdList ; FieldDecMore     */
/*                             | ArrayType IdList; FieldDecMore     */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode  FieldDecList()
{
    TreeNode t = newNode("DecK");
    TreeNode p = null;
    if (t != null)
    {
        if ((token.Lex.equals("INTEGER"))||(token.Lex.equals("CHAR")))
        {
                    BaseType(t);
	            IdList(t);
	            match("SEMI");
	            p = FieldDecMore();
        }
	else if (token.Lex.equals("ARRAY")) 
             {
	            ArrayType(t);
	            IdList(t);
	            match("SEMI");
	            p = FieldDecMore();
             }
             else
             {
		    ReadNextToken();
		    syntaxError("type name is expected");
             }
        t.sibling = p;
    }	    
    return t;	
}
/********************************************************************/
/* 函数名 FieldDecMore		 				    */
/* 功  能 类型声明部分的处理函数			            */
/* 产生式 < FieldDecMore > ::=  ε | FieldDecList                   */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode FieldDecMore()
{
    TreeNode t = null;   
    if (token.Lex.equals("INTEGER")||token.Lex.equals("CHAR")                          ||token.Lex.equals("ARRAY"))
	t = FieldDecList();
    else if (token.Lex.equals("END")) {}
	 else
             ReadNextToken();
    return t;	
}
/********************************************************************/
/* 函数名 IdList		 				    */
/* 功  能 类型声明部分的处理函数				    */
/* 产生式 < IdList > ::=  id  IdMore                                */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
void IdList(TreeNode  t)
{
    if (token.Lex.equals("ID"))
    {
	t.name[(t.idnum)] = token.Sem;
	t.idnum = t.idnum + 1;
        match("ID");
    }
    IdMore(t);
}

/********************************************************************/
/* 函数名 IdMore		 				    */
/* 功  能 类型声明部分的处理函数				    */
/* 产生式 < IdMore > ::=  ε |  , IdList                            */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
void IdMore(TreeNode t)
{
    if (token.Lex.equals("COMMA"))
    {
        match("COMMA");
        IdList(t);
    }
    else if (token.Lex.equals("SEMI")) {}
         else
	     ReadNextToken();	
}

/**************************变量声明部分******************************/
/********************************************************************/
/* 函数名 VarDec		 				    */
/* 功  能 变量声明部分的处理				            */
/* 产生式 < VarDec > ::=  ε |  VarDeclaration                      */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode VarDec()
{
    TreeNode t = null;
    if (token.Lex.equals("VAR"))
        t = VarDeclaration();
    else if ((token.Lex.equals("PROCEDURE"))||(token.Lex.equals("BEGIN")))                    {}
	 else
	     ReadNextToken();
    return t;
}
/********************************************************************/
/* 函数名 VarDeclaration		 			    */
/* 功  能 变量声明部分的处理函数				    */
/* 产生式 < VarDeclaration > ::=  VAR  VarDecList                   */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode VarDeclaration()
{
    match("VAR");
    TreeNode t = VarDecList();
    if (t==null)
	syntaxError("a var declaration is expected!");
    return t;
}

/********************************************************************/
/* 函数名 VarDecList		 				    */
/* 功  能 变量声明部分的处理函数				    */
/* 产生式 < VarDecList > ::=  TypeName VarIdList; VarDecMore        */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode VarDecList()
{
    TreeNode t = newNode("DecK");
    TreeNode p = null;
    if (t != null)
    {
	TypeName(t);
	VarIdList(t);
	match("SEMI");
        p = VarDecMore();
	t.sibling = p;
    }
    return t;
}

/********************************************************************/
/* 函数名 VarDecMore		 				    */
/* 功  能 变量声明部分的处理函数				    */
/* 产生式 < VarDecMore > ::=  ε |  VarDecList                      */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode VarDecMore()
{
    TreeNode t =null;
    if ((token.Lex.equals("INTEGER"))||(token.Lex.equals("CHAR"))                        ||(token.Lex.equals("ARRAY"))||(token.Lex.equals("RECORD"))                       ||(token.Lex.equals("ID")))
	t = VarDecList();
    else if ((token.Lex.equals("PROCEDURE"))||(token.Lex.equals("BEGIN")))
	     {}
	 else
             ReadNextToken();
    return t;
}

/********************************************************************/
/* 函数名 VarIdList		 				    */
/* 功  能 变量声明部分的处理函数			            */
/* 产生式 < VarIdList > ::=  id  VarIdMore                          */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
void VarIdList(TreeNode t)
{
    if (token.Lex.equals("ID"))
    {
        t.name[(t.idnum)] = token.Sem;
	t.idnum = t.idnum + 1;
        match("ID");
    }
    else 
    {
	syntaxError("a varid is expected here!");
	ReadNextToken();
    }
    VarIdMore(t);
}

/********************************************************************/
/* 函数名 VarIdMore		 				    */
/* 功  能 变量声明部分的处理函数				    */
/* 产生式 < VarIdMore > ::=  ε |  , VarIdList                      */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
void VarIdMore(TreeNode t)
{
    if (token.Lex.equals("COMMA"))
    {   
        match("COMMA");
        VarIdList(t);
    }
    else if (token.Lex.equals("SEMI"))  {}
         else
             ReadNextToken();	
}
/****************************过程声明部分****************************/
/********************************************************************/
/* 函数名 ProcDec		 		                    */
/* 功  能 函数声明部分的处理					    */
/* 产生式 < ProcDec > ::=  ε |  ProcDeclaration                    */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode ProcDec()
{
    TreeNode t = null;
    if (token.Lex.equals("PROCEDURE"))
        t = ProcDeclaration();
    else if (token.Lex.equals("BEGIN")) {}
         else
	     ReadNextToken();
    return t;
}
/********************************************************************/
/* 函数名 ProcDeclaration		 			    */
/* 功  能 函数声明部分的处理函数				    */
/* 产生式 < ProcDeclaration > ::=  PROCEDURE ProcName(ParamList);   */
/*                                 ProcDecPart                      */
/*                                 ProcBody                         */
/*                                 ProcDecMore                      *
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode ProcDeclaration()
{
    TreeNode t = newNode("ProcDecK");
    match("PROCEDURE");
    if (token.Lex.equals("ID"))
    {
        t.name[0] = token.Sem;
	t.idnum = t.idnum+1;
	match("ID");
    }
    match("LPAREN");
    ParamList(t);
    match("RPAREN");
    match("SEMI");
    t.child[1] = ProcDecPart();
    t.child[2] = ProcBody();
    t.sibling = ProcDecMore();
    return t;
}
/********************************************************************/
/* 函数名 ProcDecMore    				            */
/* 功  能 更多函数声明中处理函数        	        	    */
/* 产生式 < ProcDecMore > ::=  ε |  ProcDeclaration                */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode ProcDecMore()
{
    TreeNode t = null;
    if (token.Lex.equals("PROCEDURE"))
        t = ProcDeclaration();
    else if (token.Lex.equals("BEGIN"))  {}
	 else
             ReadNextToken();
    return t;
}
/********************************************************************/
/* 函数名 ParamList		 				    */
/* 功  能 函数声明中参数声明部分的处理函数	        	    */
/* 产生式 < ParamList > ::=  ε |  ParamDecList                     */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
void ParamList(TreeNode t)     
{
    TreeNode p = null;
    if ((token.Lex.equals("INTEGER"))||(token.Lex.equals("CHAR"))||                      (token.Lex.equals("ARRAY"))||(token.Lex.equals("RECORD"))||                       (token.Lex.equals("ID"))||(token.Lex.equals("VAR")))
    {
        p = ParamDecList();
        t.child[0] = p;
    } 
    else if (token.Lex.equals("RPAREN")) {} 
	 else
	     ReadNextToken();
}

/********************************************************************/
/* 函数名 ParamDecList		 			    	    */
/* 功  能 函数声明中参数声明部分的处理函数	        	    */
/* 产生式 < ParamDecList > ::=  Param  ParamMore                    */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode ParamDecList()
{
    TreeNode t = Param();
    TreeNode p = ParamMore();
    if (p!=null)
        t.sibling = p;
    return t;
}

/********************************************************************/
/* 函数名 ParamMore		 			    	    */
/* 功  能 函数声明中参数声明部分的处理函数	        	    */
/* 产生式 < ParamMore > ::=  ε | ; ParamDecList                    */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode ParamMore()
{
    TreeNode t = null;
    if (token.Lex.equals("SEMI"))
    {
        match("SEMI");
        t = ParamDecList();
	if (t==null)
           syntaxError("a param declaration is request!");
    }
    else if (token.Lex.equals("RPAREN"))  {} 
	 else
	     ReadNextToken();
    return t;
}
/********************************************************************/
/* 函数名 Param		 			    	            */
/* 功  能 函数声明中参数声明部分的处理函数	        	    */
/* 产生式 < Param > ::=  TypeName FormList | VAR TypeName FormList  */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode Param()
{
    TreeNode t = newNode("DecK");
    if ((token.Lex.equals("INTEGER"))||(token.Lex.equals("CHAR"))                        ||(token.Lex.equals("ARRAY"))||(token.Lex.equals("RECORD"))
       || (token.Lex.equals("ID")))
    {
         t.attr.procAttr = new ProcAttr();
         t.attr.procAttr.paramt = "valparamType";
	 TypeName(t);
	 FormList(t);
    }
    else if (token.Lex.equals("VAR"))
         {
             match("VAR");
             t.attr.procAttr = new ProcAttr();
             t.attr.procAttr.paramt = "varparamType";
	     TypeName(t);
	     FormList(t);
	 }
         else
             ReadNextToken();
    return t;
}

/********************************************************************/
/* 函数名 FormList		 			    	    */
/* 功  能 函数声明中参数声明部分的处理函数	        	    */
/* 产生式 < FormList > ::=  id  FidMore                             */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
void FormList(TreeNode t)
{
    if (token.Lex.equals("ID"))
    {
	t.name[(t.idnum)] = token.Sem;
	t.idnum = t.idnum + 1;
	match("ID");
    }
    FidMore(t);   
}

/********************************************************************/
/* 函数名 FidMore		 			    	    */
/* 功  能 函数声明中参数声明部分的处理函数	        	    */
/* 产生式 < FidMore > ::=   ε |  , FormList                        */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
void FidMore(TreeNode t)
{      
    if (token.Lex.equals("COMMA"))
    {
        match("COMMA");
	FormList(t);
    }
    else if ((token.Lex.equals("SEMI"))||(token.Lex.equals("RPAREN")))  
             {}
         else
	     ReadNextToken();	  
}
/********************************************************************/
/* 函数名 ProcDecPart		 			  	    */
/* 功  能 函数中的声明部分的处理函数	             	            */
/* 产生式 < ProcDecPart > ::=  DeclarePart                          */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode ProcDecPart()
{
    TreeNode t = DeclarePart();
    return t;
}

/********************************************************************/
/* 函数名 ProcBody		 			  	    */
/* 功  能 函数体部分的处理函数	                    	            */
/* 产生式 < ProcBody > ::=  ProgramBody                             */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode ProcBody()
{
    TreeNode t = ProgramBody();
    if (t==null)
	syntaxError("a program body is requested!");
    return t;
}

/****************************函数体部分******************************/
/********************************************************************/
/********************************************************************/
/* 函数名 ProgramBody		 			  	    */
/* 功  能 程序体部分的处理	                    	            */
/* 产生式 < ProgramBody > ::=  BEGIN  StmList   END                 */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode ProgramBody()
{
    TreeNode t = newNode("StmLK");
    match("BEGIN");
    t.child[0] = StmList();
    match("END");
    return t;
}

/********************************************************************/
/* 函数名 StmList		 			  	    */
/* 功  能 语句部分的处理函数	                    	            */
/* 产生式 < StmList > ::=  Stm    StmMore                           */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode StmList()
{
    TreeNode t = Stm();
    TreeNode p = StmMore();
    if (t!=null)
    {
       if (p!=null)
	   t.sibling = p;
    }
    return t;
}

/********************************************************************/
/* 函数名 StmMore		 			  	    */
/* 功  能 语句部分的处理函数	                    	            */
/* 产生式 < StmMore > ::=   ε |  ; StmList                         */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode StmMore()
{
    TreeNode t = null;
    if ((token.Lex.equals("ELSE"))||(token.Lex.equals("FI"))||                        (token.Lex.equals("END"))||(token.Lex.equals("ENDWH"))) {}
    else if (token.Lex.equals("SEMI"))
	 {
             match("SEMI");
	     t = StmList();
	 }
	 else
	     ReadNextToken();
    return t;
}
/********************************************************************/
/* 函数名 Stm   		 			  	    */
/* 功  能 语句部分的处理函数	                    	            */
/* 产生式 < Stm > ::=   ConditionalStm   {IF}                       */
/*                    | LoopStm          {WHILE}                    */
/*                    | InputStm         {READ}                     */
/*                    | OutputStm        {WRITE}                    */
/*                    | ReturnStm        {RETURN}                   */
/*                    | id  AssCall      {id}                       */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode Stm()
{
    TreeNode t = null;
    if (token.Lex.equals("IF"))
        t = ConditionalStm();
    else if (token.Lex.equals("WHILE"))         
	     t = LoopStm();
    else if (token.Lex.equals("READ"))  
	     t = InputStm();	 
    else if (token.Lex.equals("WRITE"))   
	     t = OutputStm();	 
    else if (token.Lex.equals("RETURN"))  
	     t = ReturnStm();	 
    else if (token.Lex.equals("ID"))
         {
             temp_name = token.Sem;
	     match("ID");              
             t = AssCall();
         }
	 else
	     ReadNextToken();
    return t;
}

/********************************************************************/
/* 函数名 AssCall		 			  	    */
/* 功  能 语句部分的处理函数	                    	            */
/* 产生式 < AssCall > ::=   AssignmentRest   {:=,LMIDPAREN,DOT}     */
/*                        | CallStmRest      {(}                    */  
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode AssCall()
{
    TreeNode t = null;
    if ((token.Lex.equals("ASSIGN"))||(token.Lex.equals("LMIDPAREN"))||                  (token.Lex.equals("DOT")))
	t = AssignmentRest();
    else if (token.Lex.equals("LPAREN"))
	     t = CallStmRest();
	 else 
	     ReadNextToken();
    return t;
}

/********************************************************************/
/* 函数名 AssignmentRest		 			    */
/* 功  能 赋值语句部分的处理函数	                    	    */
/* 产生式 < AssignmentRest > ::=  VariMore : = Exp                  */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode AssignmentRest()
{
    TreeNode t = newStmtNode("AssignK");
	
    /* 赋值语句节点的第一个儿子节点记录赋值语句的左侧变量名，
    /* 第二个儿子结点记录赋值语句的右侧表达式*/

    /*处理第一个儿子结点，为变量表达式类型节点*/
    TreeNode c = newExpNode("VariK");
    c.name[0] = temp_name;
    c.idnum = c.idnum+1;
    VariMore(c);
    t.child[0] = c;
		
    match("ASSIGN");
	  
    /*处理第二个儿子节点*/
    t.child[1] = Exp(); 
				
    return t;
}

/********************************************************************/
/* 函数名 ConditionalStm		 			    */
/* 功  能 条件语句部分的处理函数	                    	    */
/* 产生式 <ConditionalStm>::=IF RelExp THEN StmList ELSE StmList FI */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode ConditionalStm()
{
    TreeNode t = newStmtNode("IfK");
    match("IF");
    t.child[0] = Exp();
    match("THEN");
    if (t!=null)  
        t.child[1] = StmList();
    if(token.Lex.equals("ELSE"))
    {
	match("ELSE");   
	t.child[2] = StmList();
    }
    match("FI");
    return t;
}

/********************************************************************/
/* 函数名 LoopStm          		 			    */
/* 功  能 循环语句部分的处理函数	                    	    */
/* 产生式 < LoopStm > ::=   WHILE RelExp DO StmList ENDWH           */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode LoopStm()
{
    TreeNode t = newStmtNode("WhileK");
    match("WHILE");
    t.child[0] = Exp();
    match("DO");
    t.child[1] = StmList();
    match("ENDWH");
    return t;
}

/********************************************************************/
/* 函数名 InputStm          		     	                    */
/* 功  能 输入语句部分的处理函数	                    	    */
/* 产生式 < InputStm > ::=  READ(id)                                */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode InputStm()
{
    TreeNode t = newStmtNode("ReadK");
    match("READ");
    match("LPAREN");
    if (token.Lex.equals("ID"))	
    {
	t.name[0] = token.Sem;
        t.idnum = t.idnum+1;
    }
    match("ID");
    match("RPAREN");
    return t;
}

/********************************************************************/
/* 函数名 OutputStm          		     	                    */
/* 功  能 输出语句部分的处理函数	                    	    */
/* 产生式 < OutputStm > ::=   WRITE(Exp)                            */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode OutputStm()
{
    TreeNode t = newStmtNode("WriteK");
    match("WRITE");
    match("LPAREN");
    t.child[0] = Exp();
    match("RPAREN");
    return t;
}

/********************************************************************/
/* 函数名 ReturnStm          		     	                    */
/* 功  能 返回语句部分的处理函数	                    	    */
/* 产生式 < ReturnStm > ::=   RETURN(Exp)                           */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode ReturnStm()
{
    TreeNode t = newStmtNode("ReturnK");
    match("RETURN");
    return t;
}

/********************************************************************/
/* 函数名 CallStmRest          		     	                    */
/* 功  能 函数调用语句部分的处理函数	                  	    */
/* 产生式 < CallStmRest > ::=  (ActParamList)                       */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode CallStmRest()
{
    TreeNode t=newStmtNode("CallK");
    match("LPAREN");
    /*函数调用时，其子节点指向实参*/
    /*函数名的结点也用表达式类型结点*/
    TreeNode c = newExpNode("VariK"); 
    c.name[0] = temp_name;
    c.idnum = c.idnum+1;
    t.child[0] = c;
    t.child[1] = ActParamList();
    match("RPAREN");
    return t;
}

/********************************************************************/
/* 函数名 ActParamList          		   	            */
/* 功  能 函数调用实参部分的处理函数	                	    */
/* 产生式 < ActParamList > ::=     ε |  Exp ActParamMore           */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode ActParamList()
{
    TreeNode t = null;
    if (token.Lex.equals("RPAREN"))  {}
    else if ((token.Lex.equals("ID"))||(token.Lex.equals("INTC")))
	 {
	     t = Exp();
             if (t!=null)
	         t.sibling = ActParamMore();
         }
	 else
             ReadNextToken();
    return t;
}

/********************************************************************/
/* 函数名 ActParamMore          		   	            */
/* 功  能 函数调用实参部分的处理函数	                	    */
/* 产生式 < ActParamMore > ::=     ε |  , ActParamList             */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点  */
/********************************************************************/
TreeNode ActParamMore()
{
    TreeNode t = null;
    if (token.Lex.equals("RPAREN"))  {}
    else if (token.Lex.equals("COMMA"))
         {
	     match("COMMA");
	     t = ActParamList();
	 }
	 else	
	     ReadNextToken();
    return t;
}

/*************************表达式部分********************************/
/*******************************************************************/
/* 函数名 Exp							   */
/* 功  能 表达式处理函数					   */
/* 产生式 Exp ::= simple_exp | 关系运算符  simple_exp              */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点 */
/*******************************************************************/
TreeNode Exp()
{
    TreeNode t = simple_exp();

    /* 当前单词token为逻辑运算单词LT或者EQ */
    if ((token.Lex.equals("LT"))||(token.Lex.equals("EQ"))) 
    {
        TreeNode p = newExpNode("OpK");

	/* 将当前单词token(为EQ或者LT)赋给语法树节点p的运算符成员attr.op*/
	p.child[0] = t;
        p.attr.expAttr.op = token.Lex;
        t = p;
 
        /* 当前单词token与指定逻辑运算符单词(为EQ或者LT)匹配 */ 
        match(token.Lex);

        /* 语法树节点t非空,调用简单表达式处理函数simple_exp()	   
           函数返回语法树节点给t的第二子节点成员child[1]  */ 
        if (t!=null)
            t.child[1] = simple_exp();
    }
    return t;
}

/*******************************************************************/
/* 函数名 simple_exp						   */
/* 功  能 表达式处理						   */
/* 产生式 simple_exp ::=   term  |  加法运算符  term               */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点 */
/*******************************************************************/
TreeNode simple_exp()
{
    TreeNode t = term();

    /* 当前单词token为加法运算符单词PLUS或MINUS */
    while ((token.Lex.equals("PLUS"))||(token.Lex.equals("MINUS")))
    {
	TreeNode p = newExpNode("OpK");
	p.child[0] = t;
        p.attr.expAttr.op = token.Lex;
        t = p;

        match(token.Lex);

	/* 调用元处理函数term(),函数返回语法树节点给t的第二子节点成员child[1] */
        t.child[1] = term();
    }
    return t;
}

/********************************************************************/
/* 函数名 term						            */
/* 功  能 项处理函数						    */
/* 产生式 < 项 > ::=  factor | 乘法运算符  factor		    */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点 */
/********************************************************************/
TreeNode term()
{
    TreeNode t = factor();

    /* 当前单词token为乘法运算符单词TIMES或OVER */
    while ((token.Lex.equals("TIMES"))||(token.Lex.equals("OVER")))
    {
	TreeNode p = newExpNode("OpK");
	p.child[0] = t;
        p.attr.expAttr.op = token.Lex;
        t = p;	
        match(token.Lex);
        p.child[1] = factor();    
    }
    return t;
}

/*********************************************************************/
/* 函数名 factor						     */
/* 功  能 因子处理函数						     */
/* 产生式 factor ::= INTC | Variable | ( Exp )                       */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点 */
/*********************************************************************/
TreeNode factor()
{
    TreeNode t = null;
    if (token.Lex.equals("INTC")) 
    {
        t = newExpNode("ConstK");

	/* 将当前单词名tokenString转换为整数赋给t的数值成员attr.val */
        t.attr.expAttr.val = Integer.parseInt(token.Sem);
        match("INTC");
    }
    else if (token.Lex.equals("ID"))  	  
	     t = Variable();
    else if (token.Lex.equals("LPAREN")) 
	 {
             match("LPAREN");					
             t = Exp();
             match("RPAREN");					
         }
         else 			
             ReadNextToken();	  
    return t;
}


/********************************************************************/
/* 函数名 Variable						    */
/* 功  能 变量处理函数						    */
/* 产生式 Variable   ::=   id VariMore                   	    */
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点 */
/********************************************************************/
TreeNode Variable()
{
    TreeNode t = newExpNode("VariK");
    if (token.Lex.equals("ID"))
    {
	t.name[0] = token.Sem;
        t.idnum = t.idnum+1;
    }
    match("ID");
    VariMore(t);
    return t;
}

/********************************************************************/
/* 函数名 VariMore						    */
/* 功  能 变量处理						    */
/* 产生式 VariMore   ::=  ε                             	    */
/*                       | [Exp]            {[}                     */
/*                       | . FieldVar       {DOT}                   */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点 */
/********************************************************************/	
void VariMore(TreeNode t)
{
        if ((token.Lex.equals("EQ"))||(token.Lex.equals("LT"))||                       (token.Lex.equals("PLUS"))||(token.Lex.equals("MINUS"))||                           (token.Lex.equals("RPAREN"))||(token.Lex.equals("RMIDPAREN"))||                     (token.Lex.equals("SEMI"))||(token.Lex.equals("COMMA"))|| 
           (token.Lex.equals("THEN"))||(token.Lex.equals("ELSE"))||                       (token.Lex.equals("FI"))||(token.Lex.equals("DO"))||(token.Lex.equals           ("ENDWH"))||(token.Lex.equals("END"))||(token.Lex.equals("ASSIGN"))||           (token.Lex.equals("TIMES"))||(token.Lex.equals("OVER")))    {}
	else if (token.Lex.equals("LMIDPAREN"))
             {
	         match("LMIDPAREN");
	         t.child[0] = Exp();
                 t.attr.expAttr.varkind = "ArrayMembV";
	         match("RMIDPAREN");
	     }
	else if (token.Lex.equals("DOT"))
             {
	         match("DOT");
	         t.child[0] = FieldVar();
                 t.attr.expAttr.varkind = "FieldMembV";
	     }
	     else
	         ReadNextToken();
}
/********************************************************************/
/* 函数名 FieldVar						    */
/* 功  能 变量处理函数				                    */
/* 产生式 FieldVar   ::=  id  FieldVarMore                          */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点 */
/********************************************************************/
TreeNode FieldVar()
{
    TreeNode t = newExpNode("VariK");
    if (token.Lex.equals("ID"))
    {
	t.name[0] = token.Sem;
        t.idnum = t.idnum+1;
    }	
    match("ID");	
    FieldVarMore(t);
    return t;
}

/********************************************************************/
/* 函数名 FieldVarMore  			                    */
/* 功  能 变量处理函数                                              */
/* 产生式 FieldVarMore   ::=  ε| [Exp]            {[}              */ 
/* 说  明 函数根据文法产生式,调用相应的递归处理函数,生成语法树节点 */
/********************************************************************/
void FieldVarMore(TreeNode t)
{
    if ((token.Lex.equals("ASSIGN"))||(token.Lex.equals("TIMES"))||               (token.Lex.equals("EQ"))||(token.Lex.equals("LT"))||                          (token.Lex.equals("PLUS"))||(token.Lex.equals("MINUS"))||                      (token.Lex.equals("OVER"))||(token.Lex.equals("RPAREN"))||               (token.Lex.equals("SEMI"))||(token.Lex.equals("COMMA"))||               (token.Lex.equals("THEN"))||(token.Lex.equals("ELSE"))||               (token.Lex.equals("FI"))||(token.Lex.equals("DO"))||
       (token.Lex.equals("ENDWH"))||(token.Lex.equals("END")))
       {}
    else if (token.Lex.equals("LMIDPAREN"))
         { 
	     match("LMIDPAREN");
	     t.child[0] = Exp();
             t.child[0].attr.expAttr.varkind = "ArrayMembV";
	     match("RMIDPAREN");
	 }
	 else
	     ReadNextToken();
}

/********************************************************************/
/********************************************************************/
/* 函数名 match							    */
/* 功  能 终极符匹配处理函数				            */
/* 说  明 函数参数expected给定期望单词符号与当前单词符号token相匹配 */
/*        如果不匹配,则报非期望单词语法错误			    */
/********************************************************************/
void match(String expected)
{ 
      if (token.Lex.equals(expected))   
	  ReadNextToken();
      else 
      {
	  syntaxError("not match error ");
	  ReadNextToken();
      }     
}

/************************************************************/
/* 函数名 syntaxError                                       */
/* 功  能 语法错误处理函数		                    */
/* 说  明 将函数参数message指定的错误信息输出               */	
/************************************************************/
void syntaxError(String s)     /*向错误信息.txt中写入字符串*/
{
    serror=serror+"\n>>> ERROR :"+"Syntax error at "                              +String.valueOf(token.lineshow)+": "+s; 

    /* 设置错误追踪标志Error为TRUE,防止错误进一步传递 */
    Error = true;
}

/********************************************************************/
/* 函数名 ReadNextToken                                             */
/* 功  能 从Token序列中取出一个Token				    */
/* 说  明 从文件中存的Token序列中依次取一个单词，作为当前单词       */	
/********************************************************************/ 
void ReadNextToken()
{
    if (fenxi.hasMoreTokens())
    {
        int i=1;
	String stok=fenxi.nextToken();
        StringTokenizer fenxi1=new StringTokenizer(stok,":,");
        while (fenxi1.hasMoreTokens())
        {
            String fstok=fenxi1.nextToken();
            if (i==1)
            {
	        token.lineshow=Integer.parseInt(fstok);
	        lineno=token.lineshow;
            }
            if (i==2)
	        token.Lex=fstok;
            if (i==3)
                token.Sem=fstok;
            i++;
        }
    }    
}

/********************************************************
 *********以下是创建语法树所用的各类节点的申请***********
 ********************************************************/
/********************************************************/
/* 函数名 newNode				        */	
/* 功  能 创建语法树节点函数			        */
/* 说  明 该函数为语法树创建一个新的结点      	        */
/*        并将语法树节点成员赋初值。 s为ProcK, PheadK,  */
/*        DecK, TypeK, VarK, ProcDecK, StmLK	        */
/********************************************************/
TreeNode newNode(String s)
{
    TreeNode t=new TreeNode();
    t.nodekind = s;
    t.lineno = lineno;
    return t;
}
/********************************************************/
/* 函数名 newStmtNode					*/	
/* 功  能 创建语句类型语法树节点函数			*/
/* 说  明 该函数为语法树创建一个新的语句类型结点	*/
/*        并将语法树节点成员初始化			*/
/********************************************************/
TreeNode newStmtNode(String s)
{
    TreeNode t=new TreeNode();
    t.nodekind = "StmtK";
    t.lineno = lineno;
    t.kind = s;
    return t;
}
/********************************************************/
/* 函数名 newExpNode					*/
/* 功  能 表达式类型语法树节点创建函数			*/
/* 说  明 该函数为语法树创建一个新的表达式类型结点	*/
/*        并将语法树节点的成员赋初值			*/
/********************************************************/
TreeNode newExpNode(String s)
{
    TreeNode t=new TreeNode();
    t.nodekind = "ExpK";
    t.kind = s;
    t.lineno = lineno;
    t.attr.expAttr = new ExpAttr();
    t.attr.expAttr.varkind = "IdV";
    t.attr.expAttr.type = "Void";
    return t;
}
}



