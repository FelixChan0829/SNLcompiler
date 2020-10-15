package mycompiler.jieshiqi;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.applet.*;
/* ָ��ṹ����:������,������1,������2,������3 */
class Instruction
{
    String iop;
    int iarg1;
    int iarg2;
    int iarg3;
} 
/*���봰��*/
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
        button1=new Button("ȷ��");
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
/*��ʾ����*/
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
/* ��  �� Translator	                             */
/* ��  �� �ܳ���Ĵ���				     */
/* ˵  �� ����һ���࣬�����ܳ���                     */
/*****************************************************/
public class Translator extends Applet implements ActionListener
{
/***************** ���� *******************/

/* Ϊ���ͳ�����չ,ָ��洢����С,����Ϊ1024 */
int IADDR_SIZE = 1024;
/* Ϊ���ͳ�����չ,���ݴ洢����С,����Ϊ1024 */
int DADDR_SIZE = 1024; 
/* �Ĵ�������,����Ϊ8 */
int NO_REGS = 8;
/* PC�Ĵ���,����Ϊ7 */
int PC_REG = 7;
/* Ŀ������д�С,����Ϊ121 */
int LINESIZE = 121;
/* �ִ�С,����Ϊ20 */
int WORDSIZE = 20;

/******** ���� ********/
int iloc = 0;			/* ָ��洢����ָ��,��ʼΪ0 */
int dloc = 0;			/* ���ݴ洢����ָ��,��ʼΪ0 */
boolean traceflag = false;	/* ָ��ִ��׷�ٱ�־,��ʼΪFALSE */
boolean icountflag = false;	/* ָ��ִ�м�����־,��ʼΪFALSE */

/* iMem����ָ��洢,Ϊ1024����ָ��ṹ���� */
Instruction iMem[]=new Instruction[IADDR_SIZE];				

/* dMem�������ݴ洢,Ϊ1024���������������� */
int dMem[]=new int[DADDR_SIZE];						

/* reg���ڼĴ����洢,Ϊ8���������������� */
int reg[]=new int[NO_REGS];							

/* ָ��������,��ӦѰַģʽ��Ϊ����,��20���ַ���*/
String opCodeTab[ ] = 
{"HALT","IN","OUT","ADD","SUB","MUL","DIV","????",
"LD","ST","????", 
"LDA","LDC","JLT","JLE","JGT","JGE","JEQ","JNE","????"
};
int opRR=7;    /*��һ��"????"��λ��,����ǰ��Ϊ�Ĵ���Ѱַģʽָ������*/
int opRM=10;   /*�ڶ���"????"��λ��,����ǰ��Ϊ�Ĵ���-�ڴ�Ѱַģʽָ������*/
int opRA=19;   /*������"????"��λ��,����ǰ��Ϊ�Ĵ���-������Ѱַģʽָ������*/

/** ����ִ�н��״̬�� **/
//String stepResultTab[] = 
//{"OK","Halted","Instruction Memory Fault","Data Memory Fault","Division by 0"};

String pgm;                     /* ���ڴ洢Ŀ����� */
char in_Line[]=new char[LINESIZE];  /* ���ڴ洢һ�д��� */
int lineLen;		        /* in_Line�д���ĳ��� */
int inCol;			/* ����ָ����in_Line�еĵ�ǰ�ַ�λ�� */
int num;			/* ���ڴ洢��ǰ������ֵ */
String word;	                /* ���ڴ洢��ǰ���� */
char ch;			/* ��ǰ�������е�ǰλ���ϵ��ַ� */
String name;                    /* ��ʾ���ڵ����� */
String expr="\n";                    /* ��ʾ���ڵ����� */
int in_s;                       /* �ں���actionPerformed�뺯��stepTM�䴫��һ��intֵ */
boolean do_com=true;            /* ���������Ƿ�Ϊq(�˳�) */
String stepResult;              /* ���״̬ */
char cmd;		        /* �û����������� */
int stepcnt=0;                  /* ִ�������� */

Mywindow win1;        /* ��������� */
Mywindow win2;   /* ����ֵ���� */
XWindow xwin;           /* ��ʾ���� */

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
/* ������ tmain			            */
/* ��  �� tm����ִ�к���                    */
/* ˵  �� �������tm���������,	    */
/*	  ������ִ��Ŀ��ָ��	            */
/********************************************/ 
void tmain(String codefile)
{ 
    pgm = codefile;	

    /* Ŀ������ļ�Ϊ��,���������Ϣ */
    if (pgm == null)
    { 
        xwin=new XWindow("ERROR","TargetCode file is null");
        xwin.setVisible(true);
        return;
    }								

    /* ����ָ��:��ָ��洢��iMem��ղ���ָ�����ļ���д��ָ������ */
    if (!readInstructions())
        return;

    /* ����ִ��,�����û������TM����,���Ѿ����뵽iMem�е�ָ����в��� */
    enterCom();
}

/********************************************************/
/* ������ readInstructions				*/
/* ��  �� ָ���ļ����뺯��				*/
/* ˵  �� ��ָ���ļ��е�ָ���������뵽ָ��洢��iMem	*/
/********************************************************/
boolean readInstructions()
{ 
    int op;		        /* ��ǰָ���������opCodeTab[]�е�λ�� */
    int arg1=0,arg2=0,arg3=0;		/* ��ǰָ������� */
    int loc,regNo,lineNo;

    /* ��8���Ĵ������ݳ�ʼ��Ϊ0 */
    for (regNo = 0;regNo < NO_REGS;regNo++)
        reg[regNo] = 0;						

    /* dMemΪ���ݴ洢��,0��ַ��ԪdMem[0]��ֵ��Ϊ���ݴ洢���߶˵�ַ1023	*
     * ����ֵ����Ŀ���������ʱ�ɳ��������ָ����뵽mp�Ĵ�����	*/
    dMem[0] = DADDR_SIZE - 1;				

    /* �����ݴ洢�����ڳ�0��ַ��Ԫ��ĸ���Ԫ��ʼ��Ϊ0 */
    for (loc = 1;loc < DADDR_SIZE;loc++)
        dMem[loc] = 0;

    /* ��ָ��洢���и���Ԫ��ʼ��Ϊָ��;HALT 0,0,0 */
    for (loc = 0 ; loc < IADDR_SIZE ; loc++)
    { 
        iMem[loc]=new Instruction();
        iMem[loc].iop = "HALT";
        iMem[loc].iarg1 = 0;
        iMem[loc].iarg2 = 0;
        iMem[loc].iarg3 = 0;
    }

    lineNo = 0;		/* lineNo���ڼ�¼��ǰ����ָ���к� */

    /*��\nΪ�ָ���,��Ŀ�����ֳ�������*/
    StringTokenizer LineCode=new StringTokenizer(pgm,"\n");
    while (LineCode.hasMoreTokens())				
    { 
	String lineTok=LineCode.nextToken();
        lineLen=lineTok.length();
        in_Line=lineTok.toCharArray();

        inCol = 0;		/* ��ǰ������in_Line�е�ǰ�ַ�λ��inCol��ʼΪ0 */
        lineNo++;		/* ��ǰ�������кż�1 */

	/* ��ǰ�ַ�����"*",������ע�����,Ӧ����ָ����� */
        if((nonBlank()) && (in_Line[inCol] != '*'))
        {
	    /* ��ǰ�ַ���������,����ַ��,�������к�lineNo */
	    if (!getNum())
                return error("Bad location",lineNo,-1);

	    /* ��������ֵ������ǰ�����ַ���loc */
	    loc = num;

	    /* �����ַ���loc����ָ��洢����ַIADDR_SIZE,���� */
            if (loc > IADDR_SIZE)
                return error("Location too large",lineNo,loc);

	    /* �����ַ���loc����ȱ��ð��,��ȱ��ð�Ŵ� */
            if (!skipCh(':'))
                return error("Missing colon", lineNo,loc);

	    /* ��ǰλ�ò��ǵ���,��ȱ��ָ�������� */
            if (!getWord())
                return error("Missing opcode",lineNo,loc);

	    /* ��ʼ���op,opָ�����������,ֵΪ0 */
            op=0;

	    /* ��������opCodeTab,�Ƚϵ�ǰ��word�е��ַ�
               ���й���20���ַ��� */
            while ((op < opRA) && (!(word.equals(opCodeTab[op]))))
                op = op+1;

	    /* ��ǰ����word��ָ���Ĳ����벻�ڲ������opCodeTab��,���Ƿ���������� */
            if(!(word.equals(opCodeTab[op])))
                return error("Illegal opcode",lineNo,loc);

	    /* �Բ��õ��Ĳ�����ֵop��Ѱַģʽ,���з��ദ�� */
            String s_op=opClass(op);
            if(s_op.equals("opclRR"))
            { 			
                /* �Ĵ���Ѱַģʽ������ */
	        /* ��һ�Ĵ�����������,��0-7֮������,	*
                 * ���������Ϣ,�к�lineNo,�����ַ���loc	*/
                if ((!getNum()) || (num < 0) || (num >= NO_REGS))
                    return error("Bad first register",lineNo,loc);

		/* ����һ������arg1��ֵΪ��ǰ��ֵnum */
                arg1 = num;

		/* ��һ��������©��","�ָ���,���� */
                if (!skipCh(','))
                    return error("Missing comma",lineNo,loc);

		/* �ڶ��Ĵ�����������,��0-7֮������,		*
		 * ���������Ϣ,�к�lineNo,�����ַ���loc	*/
                if ((!getNum()) || (num < 0) || (num >= NO_REGS))
                    return error("Bad second register",lineNo,loc);

		/* ���ڶ���������arg2��ֵΪ��ǰ��ֵnum */
		arg2 = num;

		/* �ڶ���������©��","�ָ���,���� */
                if (!skipCh(',')) 
                    return error("Missing comma", lineNo,loc);

		/* �����Ĵ�����������,��0-7֮������,���� */
                if ((!getNum()) || (num < 0) || (num >= NO_REGS))
                    return error("Bad third register",lineNo,loc);

		/* ������������arg3��ֵΪ��ǰ��ֵnum */
                arg3 = num;
            }
            else if((s_op.equals("opclRM"))||(s_op.equals("opclRA")))
            {
		/* �Ĵ���-�ڴ�Ѱַģʽ		*
		 * �Ĵ���-������Ѱַģʽ	*/
		/* ��һ�Ĵ�����������,��0-7֮������,���� */
                if ((!getNum()) || (num < 0) || (num >= NO_REGS))
                    return error("Bad first register",lineNo,loc);

 		/* ����һ������arg1��ֵΪ��ǰ��ֵnum */
		arg1 = num;

		/* ��һ��������©��","�ָ���,���� */
                if (!skipCh(','))
                    return error("Missing comma",lineNo,loc);

		/* �ڶ�ƫ�Ƶ�ַ����������,������ƫ�Ƶ�ַ,���� */
                if (!getNum())
                    return error("Bad displacement",lineNo,loc);

		/* ���ڶ�ƫ�Ƶ�ַ������arg2��ֵΪ��ǰ��ַnum */
                arg2 = num;

		/* �ڶ�ƫ�Ƶ�ַ��������©��"("������","�ָ���,���� */
                if ((!skipCh('(')) && (!skipCh(',')))
                    return error("Missing LParen or comma",lineNo,loc);

		/* �ڶ��Ĵ�����������,��0-7֮������,���� */
		if ((!getNum()) || (num < 0) || (num >= NO_REGS))
                    return error("Bad second register",lineNo,loc);

		/* ������������arg3��ֵΪ��ǰ��ֵnum */
                arg3 = num;
            }
	    /* �������ַ���loc��ָ��洢��ָ��洢��iMem */
            iMem[loc].iop = opCodeTab[op]; 
            iMem[loc].iarg1 = arg1;
            iMem[loc].iarg2 = arg2;
            iMem[loc].iarg3 = arg3;
        }
    }
    return true;
}  

/****************************************************/
/* ������ opClass				    */
/* ��  �� ָ��Ѱַģʽ���ຯ��			    */
/* ˵  �� �ú����Ը�����ָ�������ö��ֵc���з���   */
/*        ����ָ������Ѱַģʽ			    */
/****************************************************/
String opClass(int c)
{ 
    /* ���ö��ֵcС��opRRLim(7),��ָ��Ϊ�Ĵ���Ѱַģʽָ������ */
    if(c <= opRR) 
        return "opclRR";

    /* ���ö��ֵcС��opRMLim(10),��ָ��Ϊ�Ĵ���-�ڴ�Ѱַģʽָ������ */
    else if(c <= opRM) 
        return "opclRM";

    /* Ϊ�Ĵ���-������Ѱַģʽָ������ */
    else                    
       return "opclRA";
}
 
/****************************************************/
/* ������ opNum				            */
/* ��  �� ָ��Ѱַģʽ���ຯ��			    */
/* ˵  �� �ú����Ը�����ָ�������ö��ֵc���з���   */
/*        ����ָ������Ѱַģʽ			    */
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
/* ������ nonBlank				        */
/* ��  �� �ǿ��ַ���ȡ����				*/
/* ˵  �� ����ɹ��ӵ�ǰ����ȡ�÷ǿ��ַ�,��������TRUE	*/
/*	  ����,��������FALSE			        */
/********************************************************/
boolean nonBlank()
{ 
    /* �ڵ�ǰ������in_Line��,��ǰ�ַ�λ��inCol��Ϊ�ո��ַ�	*   
     * �ڵ�ǰ������in_Line��,��ǰ�ַ�λ��inCol����,�Թ��ո�	*/
    while ((inCol < lineLen) && (in_Line[inCol] == ' ') )
        inCol++;

    /* �ڵ�ǰ������in_Line��,�����ǿ��ַ� */
    if (inCol < lineLen)
    { 
        /* ȡ��ǰ�ַ�λ��inCol�е��ַ�����ch,		*
	 * ��������TRUE(�Ѷ���Ϊ1),ch�еõ��ǿ��ַ�	*/
	ch = in_Line[inCol];
        return true; 
    }
    /* ��ǰ�������Ѿ�����,����ǰ�ַ�ch ��Ϊ�ո�,	*
     * ��������FALSE(�Ѷ���Ϊ0),ch��Ϊ�ո��ַ�	*/
    else
    { 
        ch = ' ';
        return false; 
    }
} 

/****************************************************/
/* ������ getCh					    */
/* ��  �� �ַ���ȡ����				    */
/* ˵  �� �����ǰ�����ַ�δ����,�������ص�ǰ�ַ� */
/*	  ����,�������ؿո��ַ�			    */
/****************************************************/
void getCh()
{ 
    /* �ڵ�ǰ������in_Line��,��ǰ�ַ�����inColδ����������ʵ�ʳ���lineLen *
     * ȡ�õ�ǰ���е�ǰλ�õ��ַ�,����ch		*/
    if (++inCol < lineLen)
        ch = in_Line[inCol];

    /* ���inCol������ǰ�����г��ȷ�Χ,��ch��Ϊ�ո� */
    else ch = ' ';
} 

/****************************************************************/
/* ������ getNum						*/
/* ��  �� ��ֵ��ȡ����						*/
/* ˵  �� �����������������ֵ��мӼ��������term�ϲ�����,       */
/*        ������ֵ����Ϊnum.����ɹ��õ���ֵ,��������TRUE;	*/
/*        ����,��������FALSE					*/
/****************************************************************/
boolean getNum()
{ 
    int sign;				/* �������� */
    int term;				/* ���ڼ�¼��ǰ¼��ľֲ���ֵ */
    boolean temp = false;		/* ��¼��������ֵ,��ʼΪ�� */
    num = 0;				/* ���ڼ�¼���мӼ�������������ֵ��� */

    do
    { 
        sign = 1;			/* �������ӳ�ʼΪ1 */

        /* ���ú���nonBlank()�Թ���ǰλ�õĿո��,			*
         * ���õ��ĵ�ǰ�ǿ��ַ�chΪ+��-.(+/-���������ִ���)	*/
        while (nonBlank() && ((ch == '+') || (ch == '-')))
        { 
            temp = false;

	    /* ��ǰ�ַ�chΪ"-"ʱ,��������sign��Ϊ-1 */
	    if(ch == '-')  
                sign = - sign;

	    /* ȡ��ǰ����������һ�ַ�����ǰ�ַ�ch�� */
            getCh();
        }
        term = 0;		/* ��ǰ¼��ľֲ���ֵ��ʼΪ0 */
        nonBlank();		/* �Թ���ǰλ���ϵĿո� */

	/* ��ǰ�ַ�chΪ����,�ֲ���ֵ��ѭ������ */
        while (isdigit(ch))				
        { 
            temp = true;		/* ��������ֵ��ΪTRUE,�ɹ��õ����� */

	    /* ���ַ�����ת��Ϊ��ֵ��ʽ,���н�λ�ۼ� */
            term = term * 10 + ( (int)ch - (int)('0') );

            getCh();			/* ȡ��ǰ����������һ�ַ�����ǰ�ַ�ch�� */

        }
	/* ���ֲ���ֵ�������ۼ�,�õ�������ֵnum */
        num = num + (term * sign);
    } while ((nonBlank()) && ((ch == '+') || (ch == '-')));
    return temp;
}

/****************************************************/
/* ������  isdigit				    */
/* ��  ��  ������c�ǲ�������			    */
/* ˵  ��  					    */
/****************************************************/
boolean isdigit(char c)
{
    if ((c=='0')||(c=='1')||(c=='2')||(c=='3')||(c=='4')||(c=='5')||(c=='6')||        (c=='7')||(c=='8')||(c=='9'))
        return true;
    else return false;
}

/****************************************************/
/* ������ getWord				    */
/* ��  �� ���ʻ�ȡ����				    */
/* ˵  �� �����ӵ�ǰ�������л�ȡ����.����õ��ַ�,  */
/*	  ��������TRUE;����,��������FALSE	    */
/****************************************************/
boolean getWord()
{ 	
    boolean temp = false;			/* ��������ֵ��ʼΪFALSE */
    int length = 0;			/* ���ʳ��ȳ�ʼΪ0 */
    char gword[]=new char[20];

    /* �ڵ�ǰ�������гɹ���ȡ�ǿ��ַ�ch */
    if(nonBlank())
    {
        /* ��ǰ�ǿ��ַ�chΪ��ĸ������ */
	while(isalpha(ch))
        {
            /* ��ǰ����wordδ�����涨�ֳ�WORDSIZE-1(Ϊ���ʽ����ַ���һ��λ)	*
	     * ����ǰ�ַ�ch���뵽����ĩβ		*/
	    if (length < WORDSIZE-1)
                gword[length++] = ch;

            getCh();			/* ȡ��ǰ����������һ�ַ� */
        }
	/* ����ǰ����word��������ַ� */
        word=new String(gword);
        word=word.trim();

	/* ���ú�������ֵ,��������word�ǿյ�ʱ��ΪTRUE */
        temp = (length != 0);
    }
    return temp;
} 

/****************************************************/
/* ������  isalpha				    */
/* ��  ��  ������c�ǲ�����ĸ			    */
/* ˵  ��  					    */
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
/* ������ skipCh					    */
/* ��  �� �ַ��չ�����					    */
/* ˵  �� �����ǰλ�����ַ�Ϊ����ָ�����ַ�,��չ����ַ�,  */
/*        ��������TRUE;����������FALSE	            */
/************************************************************/
boolean skipCh(char c)
{ 
    boolean temp = false;

    /* ��ǰλ�����ַ�Ϊ����ָ���ַ�c */
    if(nonBlank() && (ch == c))
    { 
        getCh();        /* �չ���ǰ�ַ�c,ȡ��һ�ַ� */
        temp = true;	/* �չ�ָ���ַ�c,��������TRUE */
    }
    return temp;
} 

/************************************/
/* ������ atEOL			    */
/* ��  �� �н����жϺ���	    */
/* ˵  �� ��ǰ���Ƿ�������жϺ���  */
/************************************/	
boolean atEOL()
{ 
    return (!nonBlank());	/* �����ǰ����û�зǿ��ַ�,��������TRUE */
} 

/****************************************************/
/* ������ error					    */
/* ��  �� ��������				    */
/* ˵  �� ������������к�,ָ���ַ��źʹ�����Ϣ   */
/****************************************************/
boolean error(String msg,int lineNo,int instNo)
{ 
    String s;
    s="Line "+String.valueOf(lineNo);

    /* �������ָ���ַ���instNo */
    if (instNo >= 0) 
        s=s+" (Instruction "+String.valueOf(instNo)+")";

    /* ���������Ϣmsg */
    s=s+"   "+msg+"\n";

    xwin=new XWindow("ERROR",s);
    xwin.setVisible(true);

    return false;
}

/****************************************************/
/* ������ enterCom				    */
/* ��  �� ����ָ���				    */
/* ˵  �� ����ִ��,�����û������TM����             */
/****************************************************/
void enterCom()
{
    win1=new Mywindow("Enter command: ");
    /* ��Ļ��ʾ��ʾ��Ϣ,��ʾ�û�����TM���� */
    win1.setVisible(true);
    (win1.button1).addActionListener(this);
}

/****************************************************/
/* ������ enterData			            */
/* ��  �� ����ָ���				    */
/* ˵  �� ����ִ��,�����û��������ֵ               */
/****************************************************/
void enterData()
{
    win2=new Mywindow("Enter value for IN instruction: ");
    /* ��Ļ��ʾ��ʾ��Ϣ,��ʾ�û�����TM���� */
    win2.setVisible(true);
    (win2.button1).addActionListener(this);
}

/****************************************************/
/* ������ actionPerformed			    */
/* ��  �� �����¼��ӿں���		            */
/* ˵  �� �����¼��ӿ�   		            */
/****************************************************/
public void actionPerformed(ActionEvent e)
{
    if(e.getSource()==win1.button1)   /*���������*/
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
    else if(e.getSource()==win2.button1)  /*������ֵ����*/
    {
        win2.stext=win2.text1.getText();
        lineLen = win2.stext.length();
        in_Line = win2.stext.toCharArray();
        inCol = 0;
        if(getNum())
        {
            reg[in_s] = num;
            win2.setVisible(false);
            if(cmd!='g')   /*���������ĸ�ѭ��*/
            {
        	/* stepcnt��ʱ���ڼ�¼��Ҫִ��,�����ָ������ݵ�����,�Լ� */
                stepcnt--;
                if((stepcnt > 0) && (stepResult.equals("OKAY")))
                {
    		    /* ȡ�ó��������reg[PC_REG]�е�ǰָ���ַ */
    		    iloc = reg[PC_REG];

    		    /* ����ִ��ָ��׷�ٱ�־traceflag,����ǰָ���ַiloc��ָ���������Ļ */
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
    		/* ִ�й�ָ�����stepcnt��1 */
    		stepcnt++;

    		if(stepResult.equals("OKAY"))
    		{
           	    /* ����ִ��ָ��׷�ٱ�־traceflag,����ǰ��ַiloc��ָ���������Ļ */
    		    iloc = reg[PC_REG];

    		    /* ����ִ��ָ��׷�ٱ�־traceflag,����ǰָ���ַiloc��ָ���������Ļ */
    		    if(traceflag) 
    		    {
        		name=" ";
        		writeInstruction(iloc);
    		    }
                    stepTM();
   	        }
    		else
    		{
		    /* ����ִ��ִ������׷�ٱ�־icountflag,��ʾ�Ѿ�ִ�й���ָ������ */
        	    if (icountflag)
		    {
            		name="�Ѿ�ִ�й���ָ������";
            		expr=expr+"Number of instructions executed ="+String.valueOf(stepcnt)+"\n";
        	    }
        	    step0();
    		}
    	    }
        }
        else    /*������ֵ,����*/
        {
            XWindow xwin2=new XWindow(null,"Illegal value\n");
            xwin2.setVisible(true);
        }         
    }
}

/****************************************************/
/* ������ doCom					    */
/* ��  �� ���������				    */
/* ˵  �� �����û����������                        */
/****************************************************/
void doCom()
{
    if(do_com)     /* ����������q,����ִ��,����رմ����˳� */
       doCommand();  
    else
    {
        win1.setVisible(false);
        /* ���������ִ����� */
        xwin=new XWindow("OVER","Simulation done.");
        xwin.setVisible(true);
    }
}
/****************************************************/
/* ������ doCommand				    */
/* ��  �� TM�������������			    */
/* ˵  �� ���������û������TM��������,�����Ӧ���� */
/****************************************************/
void doCommand()
{ 
    int i;
    int printcnt;
    int regNo,loc;
    name=null;

    do_com=true;
    cmd = word.charAt(0);  /* ȡ�����������еĵ�һ���ַ���cmd */
    switch(cmd)
    { 
 	/* ��������������ָ��ִ��׷�ٱ�־,׷��ָ��ִ�� */
        case 't' :
        traceflag = !traceflag;		/* ȡ������׷�ٱ�־traceflag */

        /* ���TM��t����ִ�н����Ϣ */
        String trac;
        trac="Tracing now ";
        if(traceflag) 
            trac=trac+"on.\n"; 
        else 
            trac=trac+"off.\n";

        name="׷�ٱ�־";
        expr=trac;

        break;
        /**************************************************************/

        /* ���������������Ϣ�б�,��ʾ��������书�� */
        case 'h' :

        String hel="Commands are:";

	/* ����ִ��(step)����:������"s(tep <n>"��ִ��,  *
	 * ��ִ��n(Ĭ��Ϊ1)��tmָ��.			*/
        hel=hel+"   s(tep <n>	:Execute n (default 1) TM instructions\n";

	/* ִ�е�����(go)����:������"g(o"��ִ��,*
	 * ˳��ִ��tmָ��ֱ������HALTָ��		*/
        hel=hel+"   g(o	:Execute TM instructions until HALT\n";

	/* ��ʾ�Ĵ���(regs)����:������"r(egs"��ִ��,*
	 * ��ʾ���Ĵ���������				*/
        hel=hel+"   r(egs	:Print the contents of the registers\n";

	/* ���ָ��(iMem)����:������"i(Mem <b<n>>"��ִ��,*
	 * �ӵ�ַb�����n��ָ��				*/
        hel=hel+"   i(Mem <b <n>>	:Print n iMem locations starting at b\n";

	/* �������(dMem)����:������"d(Mem<b<n>>"��ִ��,*
	 * �ӵ�ַb�����n������				*/
        hel=hel+"   d(Mem <b <n>>	:Print n dMem locations starting at b\n";

	/* ����(trace)����:������"t(race"��ִ��,	*
	 * ����׷�ٱ�־traceflag,���traceflagΪTRUE,	*
	 * ��ִ��ÿ��ָ��ʱ����ʾָ��			*/
        hel=hel+"   t(race	:Toggle instruction trace\n";

	  /* ��ʾִ��ָ������(print)����:������"p(rint)"��ִ��,	*
	   * ����׷�ٱ�־icountflag,���icountflagΪTRUE,	*
	   * ����ʾ�Ѿ�ִ�й���ָ������.ֻ��ִ��"go"����ʱ��Ч	*/
        hel=hel+"   p(rint	:Toggle print of total instructions executed"+"(go  only)"+"\n";

	  /* ����tm����(clear)����:������"c(lear"��ִ��,	*
	   * ��������tm�����,����ִ���µĳ���.			*/
        hel=hel+"   c(lear	:Reset simulator for new execution of program\n";

	  /* ����(help)����:������"h(elp"��ִ��,��ʾ�����б� */
        hel=hel+"   h(elp	:Cause this list of commands to be printed\n";

	  /* ��ֹ(quit)����,������"q(uit"��ִ��,�����������ִ�� */
        hel=hel+"   q(uit	:Terminate the simulation\n";

        name="��ʾ����书��";
        expr=hel;

        break;
        /**************************************************************/

	/* ������ʾ����ִ�й�ָ���p���� */
        case 'p' :

        icountflag = !icountflag;		/* ����ִ��ָ�������־ */

	/* ���p����ִ�еĽ����Ϣ */
        String pstr="Printing instruction count now ";
        if (icountflag) 
            pstr=pstr+"on.\n"; 
        else
            pstr=pstr+"off.\n";

        name="��ʾ����ִ�й�ָ��";
        expr=pstr;

        break;
        /**************************************************************/

	/* ����ִ��s���� */
        case 's' :

	/* ȱʡ������ģʽ,�����������,����ִ�� */
        if (atEOL())  
            stepcnt = 1;
	/* �����������������ģʽ,ȡ�ò���stepcnt,ȡ����ֵ */
        else if (getNum()) 
        { 
            if(num>0)
                stepcnt = num;
            else 
                stepcnt = -num;
        }
	/* ���δ֪����ִ�в�����Ϣ */
        else 
        { 
            name="δ֪����ִ�в�";
            expr="Step count?\n";
	}
        break;
        /**************************************************************/


	/* ִ�е�����g���� */
        case 'g' :   
         
        stepcnt = 1;    
        break;
        /**************************************************************/

        /* ��ʾ�Ĵ�������(regs)���� */
        case 'r' :

  	/* ��ʽ����ʾ���мĴ������� */
        String rstr="\n";
        for (i = 0;i < NO_REGS;i++)
            rstr=rstr+String.valueOf(i)+":"+String.valueOf(reg[i])+"\n";

        name="��ʾ�Ĵ�������";
        expr=rstr;
        break;
        /**************************************************************/

	/* ���ָ��洢��iMem��ָ���i���� */
        case 'i' :

	/* ��ʼ�����ָ����printcntΪ1 */
	printcnt = 1;

        if(getNum())
        { 
	    /* �õ�����ĵ�һ��ִ�в���,ilocָ�����ָ��Ŀ�ʼ��ַ */
	    iloc = num;
		
	    /* �õ�����ĵڶ���ִ�в���,printcntָ�����ָ������� */
            if (getNum()) 
                printcnt = num;
             
	    /* ָ���ַiloc��ָ��洢��iMem��ַ��Χ��,			*
             * ��ָ���������printcnt����0,��ilocָ����ַ���ָ������ָ��*/
            if((iloc >= 0) && (iloc < IADDR_SIZE) && (printcnt > 0))
                name="ָ��洢����ָ��";
	    while((iloc >= 0) && (iloc < IADDR_SIZE) && (printcnt > 0))
            { 
                writeInstruction(iloc);
                iloc++;
                printcnt--;
            }
        }
	/* δ����ָ�ʼ��ַ�����ָ������ */
        else
        {
            name="δ����ָ�ʼ��ַ�����ָ������";
            expr="Instruction locations?\n";
        }
        break;
        /**************************************************************/

	/* ������ݴ洢��dMem�е����ݵ�d���� */
        case 'd' :

	printcnt = 1;
        if(getNum())
        { 
	    /* ȡ������ĵ�һִ�в���,���ݴ洢�Ŀ�ʼ��ַdloc */
	    dloc = num;

		/* ȡ������ĵڶ�ִ�в���,������ݵ�����printcnt */
            if(getNum()) 
                printcnt = num;
   
            String dstr="\n";
  	    /* �������ݵ�ַdloc�����ݴ洢��dMen��ַ��Χ��,		*
	     * �������������printcnt����0,��dlocָ����ַ���ָ������������ */
	    while((dloc >= 0) && (dloc < DADDR_SIZE) && (printcnt > 0))
            { 
                dstr=dstr+String.valueOf(dloc)+"  "+String.valueOf(dMem[dloc])+"\n";
                dloc++;
                printcnt--;
            }
            name="δ�������ݴ洢���е����ݿ�ʼ��ַ������";
            expr=dstr;
        }
	/* δ�������ݴ洢���е����ݿ�ʼ��ַ������ */
        else
	{
            name="δ�������ݴ洢���е����ݿ�ʼ��ַ������";
            expr="Data locations?\n";
	}
        break;
        /**************************************************************/

        /* ����tm������ִ���µĳ���(clear)ָ�� */
        case 'c' :

        iloc = 0;		/* ָ��洢��ַ,��ʼΪ0 */
        dloc = 0;		/* ���ݴ洢��ַ,��ʼΪ0 */
        stepcnt = 0;		/* ָ��ִ�в���,��ʼΪ0 */

	/* ��ʼ�����Ĵ���reg[]Ϊ0 */
        for(regNo = 0;regNo < NO_REGS;regNo++)
            reg[regNo] = 0;			

	/* ���ݴ洢��0��ַ��Ԫ���ڼ�¼���ݴ洢��dMem�ĸ߶˵�ַ */
        dMem[0] = DADDR_SIZE - 1;

	/* ��ʼ���������ݴ洢����ԪΪ0 */
        for(loc = 1;loc < DADDR_SIZE;loc++)
            dMem[loc] = 0;				
        break;
        /**************************************************************/

        case 'q' : 

        do_com=false;		/* ִֹͣ�в��˳����� */
        return;
        /**************************************************************/

	/* ����δ��������,���������Ϣ */
        default : 
        {
            name="δ��������";
            expr="Command "+cmd+" unknown.\n";
        }
        break;
    }  /* case */

    /******************** ����ĺ������� **********************/
    stepResult = "OKAY";		/* ����ִ�н��ΪOKAY */

    if (stepcnt > 0)
    {
        if (cmd == 'g')
        { 
            /* �˴�stepcnt��Ϊ�Ѿ�ִ�й���ָ����Ŀ */
            stepcnt = 0;
            /* ����ִ��ָ��׷�ٱ�־traceflag,����ǰ��ַiloc��ָ���������Ļ */
            iloc = reg[PC_REG];
            if(traceflag) 
            {
       		name=" ";
        	writeInstruction(iloc);
            }

            /* ����ִ�е�ǰָ��,�������stepResult */
            stepTM();
        }
        else 
        {
   	    /* ȡ�ó��������reg[PC_REG]�е�ǰָ���ַ */
    	    iloc = reg[PC_REG];

    	    /* ����ִ��ָ��׷�ٱ�־traceflag,����ǰָ���ַiloc��ָ���������Ļ */
    	    if(traceflag) 
    	    {
        	name=" ";
        	writeInstruction(iloc);
    	    }

    	    /* ִ�е�ǰָ��,�������stepResult */
    	    stepTM();
        }
    } 
    else
        step0(); 
}

/************************************************/
/* ������ step0				        */
/* ��  �� 			                */
/* ˵  �� ����ִ�����,������	                */
/************************************************/
void step0()
{
    /* ����ִ�н����ö��ֵ,��ִ�н��״̬��,��ʾ���״̬ */
    if(name==null)
    {
        name="���״̬";
        expr=expr+"\n"+"���״̬"+String.valueOf(stepResult)+"\n";
    }
    else
       expr=expr+"\n"+"���״̬"+":"+String.valueOf(stepResult)+"\n";
    xwin=new XWindow(name,expr);
    xwin.setVisible(true);

    expr="\n";
    do_com=true;
    enterCom();
}

/************************************************/
/* ������ stepTM				*/
/* ��  �� TM������ִ�к���			*/
/* ˵  �� ����Ϊһ��ָ�����ִ��,���ָ���.	*/
/************************************************/
void stepTM()
{ 
    /* currentinstruction ���ڴ洢��ǰ��ִ�е�ָ�� */
    Instruction currentinstruction;		

    int pc;			/* ��������� */
    int r=0,s=0,t=0,m=0;	/* ָ������� */  
    boolean ok;	
    String ssiop;		

    do
    {
        r=0;
        s=0;
        t=0;
        m=0;

        /* pc����Ϊ��7���Ĵ���reg[7]��ֵ,Ϊ��������� */
        pc = reg[PC_REG];						

        if((pc < 0) || (pc > IADDR_SIZE))
        {
            /* pc��ֵ����ָ��洢������Ч��ַ,��ָ��洢��,��������IMEM_ERR */
            stepResult="IMEM_ERR";
            return;
        }

        /* pc��ֵΪ��Чָ���ַ,�����������reg[PC_REG]��ֵ��1 */
        reg[PC_REG] = pc + 1;

        /* ��ָ��洢��iMem֮��ȡ����ǰָ�� */
        currentinstruction = iMem[pc];

        /* ��ȡ����ָ���Ѱַģʽ���ദ��,��ʼ������ָ����������� */
        String siop=opClass(opNum(currentinstruction.iop));
        if(siop.equals("opclRR"))
        { 
            /* �Ĵ���Ѱַģʽ */
            r = currentinstruction.iarg1;
            s = currentinstruction.iarg2;
            t = currentinstruction.iarg3;
        }
        else if(siop.equals("opclRM"))
        {		
            /* �Ĵ���-�ڴ�Ѱַģʽ */
	    r = currentinstruction.iarg1;
            s = currentinstruction.iarg3;
            m = currentinstruction.iarg2 + reg[s];
      
	    /* ������m�����ݴ洢����Ч��ַ,�����ݴ洢��,��������DMEM_ERR */
	    if((m < 0) || (m > DADDR_SIZE))
           {
                stepResult="DMEM_ERR";
                return;
           }
        }
        else if(siop.equals("opclRA"))
        {		
            /* �Ĵ���-������Ѱַģʽ */
            r = currentinstruction.iarg1;
            s = currentinstruction.iarg3;
            m = currentinstruction.iarg2 + reg[s];
        }

        /* �Խ�ִ��ָ��Ĳ�����ֵ���з��ദ��,���ָ����Ϣ,	*
         * ���ָ���,������Ӧ���״̬		*/
        ssiop=currentinstruction.iop;
        /******************** RRָ�� ******************/
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
           	 /* ��ʽ����Ļ��ʾHALT(ֹͣ)ָ��,����״̬HALT(ֹͣ) */
          	 expr=expr+"HALT: "+String.valueOf(r)+","+String.valueOf(s)+","+String.valueOf(t)+"\n";
        	 stepResult="HALT";
    	    }

    	    /**********************************************/
    	    else if(ssiop.equals("OUT")) 
       	    /* ��Ļ��ʾOUTָ��ִ�еĽ����Ϣ */ 
        	expr=expr+"OUT instruction prints: "+String.valueOf(reg[r])+"\n";

            /**********************************************/
    	    else if(ssiop.equals("ADD")) 
	 	/* ���ADDָ����� */
        	reg[r] = reg[s] + reg[t]; 

    	    /**********************************************/
            else if(ssiop.equals("SUB")) 
		/* ���SUBָ����� */
        	reg[r] = reg[s] - reg[t]; 

    	    /**********************************************/
   	    else if(ssiop.equals("MUL")) 
		/* ���MULָ����� */
        	reg[r] = reg[s] * reg[t]; 

    	    /**********************************************/
    	    else if(ssiop.equals("DIV")) 
            {
		/* ���ڳ���ָ��,������Ϊ0,�򱨳������, *
	 	* ������ZERODIVIDE;����,��ɳ������� */
		if(reg[t] != 0) 
            	    reg[r] = reg[s] / reg[t];
        	else 
           	    stepResult="ZERODIVIDE";
    	    }

   	    /***************** RM ָ�� ********************/
    	    /**********************************************/
    	    else if(ssiop.equals("LD")) 
		/* �����ݴ洢��dMem�е��������뵽�Ĵ���reg[r] */
       		 reg[r] = dMem[m]; 
 
    	    /**********************************************/
   	    else if(ssiop.equals("ST")) 
		/* ���Ĵ���reg[r]�е�����д�뵽���ݴ洢��dMem */
        	dMem[m] = reg[r];  
		
    	    /***************** RA ָ�� ********************/
    	    /**********************************************/
    	    else if(ssiop.equals("LDA")) 
		/* ���Ĵ���reg[r]��ֵΪ������m��ֵ */
        	reg[r] = m; 

    	    /**********************************************/
    	    else if(ssiop.equals("LDC")) 
		/* ���Ĵ���reg[r]��ֵΪ��ǰָ��ĵڶ���������ֵ */
        	reg[r] = currentinstruction.iarg2; 

            /**********************************************/
    	    else if(ssiop.equals("JLT")) 
    	    {
		/* ����Ĵ���reg[r]��ֵС��0,�򽫳��������reg[PC_REG]��ֵ	*
		 * ��ֵΪ������m,����С��������ת		*/
        	if(reg[r] <  0) 
            	    reg[PC_REG] = m; 
    	    }

    	    /**********************************************/
    	    else if(ssiop.equals("JLE")) 
    	    {
		/* ����Ĵ���reg[r]��ֵС�ڵ���0,�򽫳��������reg[PC_REG]��ֵ	*
		 * ��ֵΪ������m,����С�ڵ���������ת		*/
        	if(reg[r] <=  0) 
           	    reg[PC_REG] = m;
    	    }

    	    /**********************************************/
    	    else if(ssiop.equals("JGT")) 
    	    {
		/* ����Ĵ���reg[r]��ֵ����0,�򽫳��������reg[PC_REG]��ֵ	*
	 	* ��ֵΪ������m,��������������ת		*/
        	if(reg[r] >  0) 
            	    reg[PC_REG] = m; 
    	    }

    	    /**********************************************/
    	    else if(ssiop.equals("JGE"))
    	    { 
		/* ����Ĵ���reg[r]��ֵ���ڵ���0,�򽫳��������reg[PC_REG]��ֵ	*
	 	* ��ֵΪ������m,�������ڵ�����ת			*/
        	if(reg[r] >=  0) 
            	    reg[PC_REG] = m;
    	    }

    	    /**********************************************/
    	    else if(ssiop.equals("JEQ"))
    	    { 
		/* ����Ĵ���reg[r]��ֵ����0,�򽫳��������reg[PC_REG]��ֵ	*
	 	* ��ֵΪ������m,��������������ת		*/
        	if (reg[r] == 0) 
            	    reg[PC_REG] = m; 
    	    }

    	    /**********************************************/
    	    else if(ssiop.equals("JNE")) 
    	    {
		/* ����Ĵ���reg[r]��ֵ������0,�򽫳��������reg[PC_REG]��ֵ	*
	 	* ��ֵΪ������m,����������������ת			*/
        	if (reg[r] != 0) 
            	    reg[PC_REG] = m;
    	    }
    	    if(cmd!='g')   /*���������ĸ�ѭ��*/
            {
        	/* stepcnt��ʱ���ڼ�¼��Ҫִ��,�����ָ������ݵ�����,�Լ� */
                stepcnt--;
                if((stepcnt > 0) && (stepResult.equals("OKAY")))
                {
    		    /* ȡ�ó��������reg[PC_REG]�е�ǰָ���ַ */
    		    iloc = reg[PC_REG];

    		    /* ����ִ��ָ��׷�ٱ�־traceflag,����ǰָ���ַiloc��ָ���������Ļ */
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
    		/* ִ�й�ָ�����stepcnt��1 */
    		stepcnt++;

    		if(stepResult.equals("OKAY"))
    		{
           	    /* ����ִ��ָ��׷�ٱ�־traceflag,����ǰ��ַiloc��ָ���������Ļ */
    		    iloc = reg[PC_REG];

    		    /* ����ִ��ָ��׷�ٱ�־traceflag,����ǰָ���ַiloc��ָ���������Ļ */
    		    if(traceflag) 
    		    {
        		name=" ";
        		writeInstruction(iloc);
    		    }
   	        }
    		else
    		{
		    /* ����ִ��ִ������׷�ٱ�־icountflag,��ʾ�Ѿ�ִ�й���ָ������ */
        	    if (icountflag)
		    {
            		name="�Ѿ�ִ�й���ָ������";
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
/* ������ writeInstruction				*/
/* ��  �� ָ���������					*/
/* ˵  �� �ú�����ָ��洢����ָ����ָ����ʽ�������Ļ	*/
/********************************************************/
void writeInstruction(int loc)
{  
    String wstr;
    /* locΪ��Ҫ�����ָ����ָ��洢���е�ַ,�������Ļ */
    wstr=String.valueOf(loc);

    /* ���ָ���ַloc��0-1023��Ч��ָ��洢����ַ��Χ֮�� */
    if ((loc >= 0)&&(loc < IADDR_SIZE))
    { 
        /* �����ַΪloc�ϵ�ָ�������ֵiMem[loc].iop�͵�һ������iMem[loc].iarg1 */
	wstr=wstr+iMem[loc].iop+"   "+String.valueOf(iMem[loc].iarg1);

	/* ����ָ���Ѱַģʽ���ദ�� */
        String ss=opClass(opNum(iMem[loc].iop));
        if(ss.equals("opclRR"))
            /* ���ָ��Ϊ�Ĵ���Ѱַģʽָ��,�Ը�����ʽ���������2,������3 */
	    wstr=wstr+String.valueOf(iMem[loc].iarg2)+","+String.valueOf(iMem[loc].iarg3);	
        else if((ss.equals("opclRM"))||(ss.equals("opclRA")))
            /* ���ָ��Ϊ�Ĵ���-������Ѱַģʽָ��,�ͼĴ���-�ڴ�Ѱַģʽָ��	*
	     * �Ը�����ʽ���������2,������3		*/
            wstr=wstr+String.valueOf(iMem[loc].iarg2)+"("+String.valueOf(iMem[loc].iarg3)+")";	
     }
     /* ����Ļ������з� */
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
    TokenType  Token=new TokenType();      //����
    ChainNodeType nextToken=null;          //ָ����һ�����ʵ�ָ��
}
/******************************************/
class SymbTable  /* ���������ʱ�õ� */
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
class TreeNode   /* �﷨�����Ķ��� */
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
    ArrayAttr arrayAttr=null;  /* ֻ�õ�����һ�����õ�ʱ�ٷ����ڴ� */
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
/*Դ�����Ӧ���м�������б�ʾ*/
class CodeFile
{   
    CodeR codeR=new CodeR();
    CodeFile former=null;
    CodeFile next=null;
}
/*�м����Ľṹ*/
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
    MidAttr midAttr=new MidAttr();  /*������ARG�ṹ��Ҫ��¼����Ϣ*/
}  
class MidAttr		
{  
    int value;  /*��¼����ֵ*/
    int label;  /*��¼��ŵ�ֵ*/
    Addr addr;
}
class Addr
{ 
    String name;    /*ע�����������Ѿ�û�ã����ﱣ��ֻ��Ϊ����ʾ�������*/
    int dataLevel;
    int dataOff;
    String access;  /*����AccessKind��ǰ�涨��*/
}
/*������ֵ�����ڳ����ʽ�Ż�*/
class ConstDefT
{ 
    ArgRecord variable=new ArgRecord();   /*�ñ�����ARG�ṹ��ʾ����*/
    int constValue;       /*��ֵ*/
    ConstDefT former=null;
    ConstDefT next=null;
} 
/*ֵ�����ValuNum*/
class ValuNum
{
    ArgRecord arg=new ArgRecord();
    String access;
    CodeInfo codeInfo=new CodeInfo();
    /*ָ����һ���ڵ�ָ��*/
    ValuNum next=null;
}
class CodeInfo
{
    int valueCode;   /*ֱ�ӱ������洢ֵ����*/
    TwoCode twoCode=null;
}
class TwoCode
{
    int valuecode;
    int addrcode;         /*�����ʱ�������洢ֵ����͵�ַ��*/
}
/*�м�����Ӧ��ӳ����ṹ*/
class MirrorCode
{  
    int op1;
    int op2;
    int result;
} 
/*���ñ��ʽ�����UsableExpr*/
class UsableExpr
{ 
    CodeFile code=null;	   /*�м�����ַ*/
    MirrorCode mirrorC=null;    /*ӳ����*/
    UsableExpr next=null;  /*ָ����һ���ڵ�*/
} 
/*��ʱ�����ĵȼ۱�TempEqua*/
class TempEqua
{
    ArgRecord arg1=null; /*���滻����ʱ����*/
    ArgRecord arg2=null; /*�����滻����ʱ����*/
    TempEqua next=null;
} 
/*ѭ����Ϣ��*/
class LoopInfo
{
    int state;            /*ѭ��״����Ϊ0ʱ��ʾ����ѭ����������*/
    CodeFile whileEntry;  /*ָ��ѭ������м����*/
    int varDef;           /*ָ�򱾲�ѭ���ı�����ַ����ʼ��*/
    CodeFile whileEnd;    /*ָ��ѭ�������м����*/
}    
/*ѭ����Ϣջ*/
class LoopStack
{ 
    LoopInfo loopInfo;
    LoopStack under=null;
} 
/*��ŵ�ַ��*/
class LabelAddr
{
    int label;
    int destNum;
    LabelAddr next=null;
} 
/*��������ַҪ�õ������ݽṹ*/
class BackAddr
{  
    int backLoc;
    BackAddr former=null;
}
/*****************************************************/
/********************************************************************/
/* ��  �� Target	                                            */
/* ��  �� �ܳ���Ĵ���					            */
/* ˵  �� ����һ���࣬�����ܳ���                                    */
/********************************************************************/
class Target
{
BackAddr AddrTop;
LabelAddr labelAddrT;
int AddrEMPTY;

int tmpOffset = 0;           /*��ʱ��������ƫ��*/

/* TMָ�ǰ���ɴ���д���ַ */
int emitLoc = 0 ;

/* �����ں���emitSkip,emitBackup,emitRestore	
   ����Ϊ��ǰ������ɴ���д���ַ,��ʼΪ0 */
int highEmitLoc = 0;

boolean TraceCode=true;    /* ��������׷�ٱ�־ */

int mainOff;

/* ����ָ��ָʾ��pcΪ7,ָ��ǰָ��洢λ��	
   ����ָʾ����ʹ�üĴ��������еĵ�8���Ĵ���	*/
int pc=7;	

/* ���̻��¼ͷ��ַָʾ��spָ����̻��¼��ͷ��ַ*/
int sp=6;

/* ���̻��¼β��ַָʾ��topָ����̻��¼��β��ַ */
int top=5; 

/* ���̻��¼sp��display�����ָʾ��displayOff */
int displayOff=4;

/* �洢ָʾ��mpָ��������ʱ�����洢�����ݴ洢������ */
int mp=3;

int ac2=2;      /* �����ۼ��� */
int ac1=1;      /* �ڶ��ۼ��� */
int ac=0;       /* ��һ�ۼ��� */

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
/************* �����������Ļ������� *************/
/************************************************/
/* ������ codeGen				*/ 
/* ��  �� Ŀ���������������			*/		
/* ˵  �� �ú���ͨ��ɨ���м�������в���Ŀ�����*/
/************************************************/
void codeGen(CodeFile midcode)
{ 
    String s="File: Ŀ�����";

    /* ���ɴ����ļ�˵��ע��,д������ļ� */
    emitComment("SNL Compilation to TM Code");
    emitComment(s);
 
    /* ���ɱ�׼����ָ�� */
    emitComment("Standard prelude:");
   
    /* д�뵥Ԫ����ָ��,���0��ַ��Ԫ������ */
    emitRM("ST",ac,0,ac,"clear location 0");
   
    /* д��ע��,����ָ��д�� */
    emitComment("End of standard prelude.");
   
    /*Ϊ�����������һ����ת���*/
    int savedLoc = emitSkip(1);

    /*ѭ����������м���룬������Ӧ�ú���������Ӧ��Ŀ�����*/
    while  (midcode!=null)
    {
	if ((midcode.codeR.codekind.equals("ADD"))||(midcode.codeR.codekind.equals("SUB"))||(midcode.codeR.codekind.equals("MULT"))||(midcode.codeR.codekind.equals("DIV"))||(midcode.codeR.codekind.equals("LTC"))||(midcode.codeR.codekind.equals("EQC")))
	    /*���㴦��,������������͹�ϵ����*/
	    arithGen(midcode);
	else if (midcode.codeR.codekind.equals("AADD"))	
	    /*��ַ������*/
	    aaddGen(midcode);
	else if (midcode.codeR.codekind.equals("READC"))		
	    /*�������*/
	    readGen(midcode);		
	else if (midcode.codeR.codekind.equals("WRITEC"))
	    /*������*/
	    writeGen(midcode);		
	else if (midcode.codeR.codekind.equals("RETURNC"))
	    /*�������*/
            returnGen(midcode);		
	else if (midcode.codeR.codekind.equals("ASSIG"))
	    /*��ֵ���*/
	    assigGen(midcode);		
	else if ((midcode.codeR.codekind.equals("LABEL"))||(midcode.codeR.codekind.equals("WHILESTART"))||(midcode.codeR.codekind.equals("ENDWHILE")))
	    /*����������*/
	    labelGen(midcode);		
	else if (midcode.codeR.codekind.equals("JUMP"))
	    /*��ת���*/
	    jumpGen(midcode,1);	
	else if (midcode.codeR.codekind.equals("JUMP0"))
	    /*������ת���*/
	    jump0Gen(midcode);		
	else if (midcode.codeR.codekind.equals("VALACT"))
	    /*��ʵ�ν����䣺�β���ֵ��*/
	    valactGen(midcode);
	else if (midcode.codeR.codekind.equals("VARACT"))		
	    /*��ʵ�ν����䣺�β��Ǳ��*/
	    varactGen(midcode);
	else if (midcode.codeR.codekind.equals("CALL"))		
	    /*���̵������*/
	    callGen(midcode);
	else if (midcode.codeR.codekind.equals("PENTRY"))		
	    /*�����������*/
	    pentryGen(midcode);
	else if (midcode.codeR.codekind.equals("ENDPROC"))		
	    /*���̳�������*/
	    endprocGen(midcode);
	else if (midcode.codeR.codekind.equals("MENTRY"))	
	    /*��������ڴ���*/
	    mentryGen(midcode,savedLoc);
	else 
            mbcode=mbcode+" midcode  bug.\n"; 
    midcode = midcode.next;
    }

    /*�������������˳�AR*/
    emitComment("<- end of main ");
    /* д��ע��,��־�ļ�ִ�еĽ��� */
    emitComment("End of execution.");
    /* д��ָֹͣ��,��������ִ�� */
    emitRO("HALT",0,0,0,"");
}
/************************************************/
/* ������ arithGen			        */ 
/* ��  �� �������������Ŀ�����		*/		
/* ˵  ��				        */
/************************************************/
void arithGen(CodeFile midcode)
{
    /* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,��ע������ʼ */
    if (TraceCode) 	  
        emitComment("-> Op");

    /*�������������Ŀ����룬ֵ����ac��*/
    operandGen(midcode.codeR.arg1);

    /* �ݴ����������ac2�� */
    emitRM("LDA",ac2,0,ac,"op: store  left ");  
  
    /*�����Ҳ�������Ŀ����룬ֵ����ac��*/
    operandGen(midcode.codeR.arg2);

    /* ȡ�������������ac1*/
    emitRM("LDA",ac1,0,ac2,"op: load left");

    /*���ݲ����������������Ŀ����룬ac��Ϊ������*/
    if (midcode.codeR.codekind.equals("ADD"))
        /*���*/
	emitRO("ADD",ac,ac1,ac,"op +");
    else if (midcode.codeR.codekind.equals("SUB"))
	/*���*/
	emitRO("SUB",ac,ac1,ac,"op -");	
    else if (midcode.codeR.codekind.equals("MULT"))
	/*���*/
	emitRO("MUL",ac,ac1,ac,"op *");	
    else if (midcode.codeR.codekind.equals("DIV"))
	/*���*/
	emitRO("DIV",ac,ac1,ac,"op /");  
    else if (midcode.codeR.codekind.equals("LTC"))
    {
	/*С��*/
        /* д���ָ��,��(��-��)���������,������ۼ���ac */
	emitRO("SUB",ac,ac1,ac,"op <");  
        /* д���ж���תָ��,����ۼ���ac��ֵС��0,�����ָ��ָʾ����������ָ��*/
	emitRM("JLT",ac,2,pc,"br if true");
        /* д�����볣��ָ��,���ۼ���ac��ֵΪ0 */
	emitRM("LDC",ac,0,0,"false case"); 
        /* д����ֵ����ָ��,����ָ��ָʾ��pc������һ��ָ�� */
	emitRM("LDA",pc,1,pc,"unconditional jmp") ;
        /* д�����볣��ָ��,���ۼ���ac��ֵΪ1 */
	emitRM("LDC",ac,1,0,"true case");
    }
    else if (midcode.codeR.codekind.equals("EQC"))
    {
	/*����*/
	/* д�����ָ��,����,�Ҳ��������,������ۼ���ac */
	emitRO("SUB",ac,ac1,ac,"op ==");
        /* д���ж���תָ��,����ۼ���ac����0,����ָ��ָʾ��pc��������ָ��*/
	emitRM("JEQ",ac,2,pc,"br if true");
        /* д�����볣��ָ��,���ۼ���ac��ֵΪ0 */
	emitRM("LDC",ac,0,0,"false case");
        /* д����ֵ����ָ��,����ָ��ָʾ��pc����һ��ָ�� */
	emitRM("LDA",pc,1,pc,"unconditional jmp") ;
        /* д�����볣��ָ��,���ۼ���ac��ֵΪ1 */
	emitRM("LDC",ac,1,0,"true case");
    }

    /*����Ҫ��ac���ʱ���ac*/
    emitRM("LDA",ac2,0,ac,"op: store  result ");  

    /*����Ŀ�Ĳ������ĵ�ַ������ac��*/
    FindAddr(midcode.codeR.arg3);

    /*ȡ���ݴ�ļ�����������ac1*/
    emitRM("LDA",ac1,0,ac2,"op: load result");

    /*����������Ŀ�Ĳ�����*/
    emitRM("ST",ac1,0,ac, "");

    /* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע����Ϣ,��ע�������� */
    if (TraceCode)  
        emitComment("<- Op"); 
}
/************************************************/
/* ������ operandGen				*/ 
/* ��  �� ���ɲ�������Ŀ�����			*/		
/* ˵  �� �ֲ�����Ϊ�������߱��������������	*/
/*        ע�ⲻ����ac2				*/
/************************************************/
void operandGen(ArgRecord arg)
{
    if (arg.form.equals("ValueForm"))
    { 
        /*������Ϊ����*/
        /* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,�������ֿ�ʼ */
	if (TraceCode) 
            emitComment("-> Const");

	/* �������볣��ָ��,���볣�����ۼ���ac */
	emitRM("LDC",ac,arg.midAttr.value,0,"load const");
	  
	/* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,�������ֽ��� */
	if (TraceCode)  
            emitComment("<- Const");
    }
    else if (arg.form.equals("LabelForm"))
    {
        /*����Ϊ���*/
	/* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,��Ų��ֿ�ʼ */
	if (TraceCode) 
            emitComment("-> Label");

	/* ����������ָ��,������ֵ���ۼ���ac */
	emitRM("LDC",ac,arg.midAttr.label,0,"load label");
	  
	/* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,��Ų��ֽ��� */
	if (TraceCode)  
            emitComment("<- Label");
    } 
    else if (arg.form.equals("AddrForm"))
    {
	/*������Ϊ����,�п�������ʱ����*/
	/* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,��ע��ʶ����ʼ */
	if (TraceCode) 
            emitComment("-> var");
	  
	FindAddr(arg);
	/*����ac���ص���Դ��������ʱ�����ľ���ƫ��*/
	  
	if(arg.midAttr.addr.access.equals("indir"))
	{   
	    /*ȡ������Ϊ��ַ,��ȡ����*/
	    emitRM("LD",ac1,0,ac,"indir load id value");
	    emitRM("LD",ac,0,ac1,"");
	}
	else
	{   
            /*�����ֵ*/
	    /* д����ֵ����ָ��,���������ʶ����ֵ*/
	    emitRM("LD",ac,0,ac,"load id value");
	}

	/* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,��ע��ʶ������ */
	if (TraceCode)  
            emitComment("<- var");
    }
}
/************************************************/
/* ������ aaddGen				*/ 
/* ��  �� ���ɵ�ַ�Ӳ�����Ŀ�����		*/		
/* ˵  ��					*/
/************************************************/
void aaddGen(CodeFile midcode)
{	
    /* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,aadd��俪ʼ */
    if (TraceCode)  
        emitComment("->address  add");
  
    if (midcode.codeR.arg1.midAttr.addr.access.equals("dir"))
    {   
        /*ac�еĵ�ַ��Ϊ����ַ*/
	/*��������ľ���ƫ��,ac�д�Ϊ�����ľ���ƫ��*/
        FindAddr(midcode.codeR.arg1);
    }
    else
    {   
        /*ac�еĵ�ַ��ŵ�����Ϊ����ַ*/
	/*��������ľ���ƫ��,ac�д�Ϊ�����ľ���ƫ��*/
        FindAddr(midcode.codeR.arg1);
	emitRM("LD",ac,0,ac,"");
    }
    /*����ַת�浽ac2*/
    emitRM("LDA",ac2,0,ac,"op: store  baseaddr ");  

    /*���ַ��������ƫ����,����ac��*/
    operandGen(midcode.codeR.arg2);

    /*��ַ���,�����ac2��*/
    emitRO("ADD",ac2,ac2,ac,"op +");
    
    /*��Ŀ�ı����ĵ�ַ������ac*/
    FindAddr(midcode.codeR.arg3);

    /*��ַ��ӽ��д��Ŀ�ı���*/
    emitRM("ST",ac2,0,ac,"");
}
/************************************************/
/* ������ readGen				*/ 
/* ��  �� ���ɶ�������Ŀ�����			*/		
/* ˵  �� ���ݱ�����ֱ�ӱ������Ǽ�ӱ�������	*/
/*	  ��ͬ�Ĵ���				*/
/************************************************/
void readGen(CodeFile midcode)
{
    /*���ɶ�ָ���ָ����ɶ����ⲿ��ֵ���ۼ���ac2�Ķ���*/
    emitRO("IN",ac2,0,0,"read integer value");

    /*��������ľ���ƫ��,ac�д�Ϊ�����ľ���ƫ��*/
    FindAddr(midcode.codeR.arg1);
    
    if(midcode.codeR.arg1.midAttr.addr.access.equals("dir"))
    {	
        /*ֱ�Ӵ�*/
	/*������ɴ洢ָ��*/
	emitRM("ST",ac2,0,ac," var read : store value");
    }
    else
    {
	/*��ac������Ϊ��ַ�ұ�����Ԫ,�ٴ�*/
	emitRM("LD",ac1,0,ac,"");
	emitRM("ST",ac2,0,ac1," indir var read : store value");
    }
}
/************************************************/
/* ������ writeGen				*/ 
/* ��  �� ����д������Ŀ�����			*/		
/* ˵  �� ���ú����õ�ֵ���������������	*/
/************************************************/
void writeGen(CodeFile midcode)
{
    /*���ú������õ������ֵ������ac��*/
    operandGen(midcode.codeR.arg1);
	
    /*����дָ���ָ����ɽ��ۼ���ac�е�ֵ����Ķ���*/
    emitRO("OUT",ac,0,0,"write ac");
}
/************************************************/
/* ������ returnGen				*/ 
/* ��  �� ���ɷ�������Ŀ�����		*/		
/* ˵  �� ���ع��̵��õ���һ����䣬ע��return  */
/*	  ���ֻ�ڹ����г���			*/
/************************************************/
void returnGen(CodeFile midcode)
{
    /*�ӹ����������������Ĺ�������̽�����ͬ*/
    endprocGen(midcode);
}
/************************************************/
/* ������ assigGen				*/ 
/* ��  �� ���ɸ�ֵ����Ŀ�����		*/		
/* ˵  ��					*/
/************************************************/
void assigGen(CodeFile midcode)
{
    /* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,assign��俪ʼ */
    if (TraceCode)  
        emitComment("->assign");
    
    /*��ֵ�󲿱����ĵ�ַ������ac��*/
    FindAddr(midcode.codeR.arg2);
    /*ת����ac2��*/
    emitRM("LDA",ac2,0,ac,"op: store  addr ");  

    /*���ɸ�ֵ�Ҳ���Ŀ����룬ֵ����ac��*/
    operandGen(midcode.codeR.arg1);

    if(midcode.codeR.arg2.midAttr.addr.access.equals("dir"))
	/*��ֵ,ac2��Ϊ��ַ*/
	emitRM("ST",ac,0,ac2,"var assign : store value");
    else
    {
	/*��ac2��ȡ�����ݣ���Ϊ��ַ*/
	emitRM("LD",ac1,0,ac2," indir var assign");
	emitRM("ST",ac,0,ac1," store value");
    }
		
    /* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,assign������ */
    if (TraceCode)  
        emitComment("<- assign") ;
}
/************************************************/
/* ������ labelGen				*/ 
/* ��  �� �����ŵ�Ŀ���������		*/		
/* ˵  �� ������û�д˱�Ŷ�Ӧ����,���ŵ�ַ�� */
/*        �������д˱�Ŷ�Ӧ������Ŀ�����	*/
/************************************************/
void labelGen(CodeFile midcode)
{
    /*ȡ�ñ��ֵ*/
    int label = midcode.codeR.arg1.midAttr.label;

    /*ȡ�õ�ǰĿ�������*/
    int currentLoc = emitSkip(0) ;
  
    /*���ұ�ŵ�ַ��*/
    LabelAddr item = labelAddrT;
    LabelAddr last = item;
    while (item!=null)
    {	
        if (item.label == label)
	    break;
        last = item;
        item = item.next;
    }

    if (item==null)  /*����û�д˱�Ŷ�Ӧ����,��� */
    { 
        /*�½�һ��*/
	LabelAddr newItem = new LabelAddr();
        newItem.label = label;
	newItem.destNum = currentLoc;
        /*�����ŵ�ַ����*/
        if (labelAddrT == null)
	    labelAddrT = newItem;
	else 
            last.next = newItem;
    }
    else /*�����д˱�Ŷ�Ӧ������Ŀ�����*/ 
    {  
	/*�˻ص�ָ������ַ*/
	emitBackup(item.destNum);
	/*д����ת���˱������Ŀ�����λ�õĴ���*/
	emitRM("LDC",pc,currentLoc,0,"jump to label");
	/*�ָ���ǰĿ�����*/
	emitRestore();
    }
}
/************************************************/
/* ������ jumpGen				*/ 
/* ��  �� ������ת��Ŀ�����			*/		
/* ˵  �� ����i��Ϊ�˸��ô˺������裬����i	*/
/*	  �������м�������ĸ�����ȡ���ֵ	*/
/************************************************/
void jumpGen(CodeFile midcode,int i)
{
    int label;
    /*ȡ�ñ��ֵ*/
    if (i == 1)
	label = midcode.codeR.arg1.midAttr.label;
    else  
        label = midcode.codeR.arg2.midAttr.label; 
  
    /*���ұ�ŵ�ַ��*/
    LabelAddr item = labelAddrT;
    LabelAddr last = item;
    while (item!=null)
    {	
        if (item.label == label)
	    break;
	last = item;
	item = item.next;
    }

    if (item==null)  /*����û�д˱�Ŷ�Ӧ����,��� */
    {	
	/*Ԥ�������ַ*/
	int currentLoc = emitSkip(1);

	/*�½�һ��*/
	LabelAddr newItem = new LabelAddr();
	newItem.label = label;
	newItem.destNum = currentLoc;
	/*�����ŵ�ַ����*/
	if (last == null)
	    labelAddrT = newItem;
	else 
            last.next = newItem;
    }
    else 
        /*�����д˱�Ŷ�Ӧ����������Ŀ�����*/ 
	emitRM("LDC",pc,item.destNum,0,"jump to label");
}
/************************************************/
/* ������ jump0Gen				*/ 
/* ��  �� ������ת����Ŀ���������		*/		
/* ˵  ��					*/
/************************************************/
void jump0Gen(CodeFile midcode)
{   
    /*ȡ�þ����Ƿ���ת��ֵ������ac��*/
    operandGen(midcode.codeR.arg1);
    /*ת�浽ac2��*/
    emitRM("LDA",ac2,0,ac,"op: store  addr ");  

    /*�˴�Ϊ��ַ����Ԥ��һ��ָ��ռ�,���ɲ���תʱ�Ĵ���*/
    int savedLoc = emitSkip(1);

    /*������ת����,ͨ������*/
    jumpGen(midcode,2);

    /*ָ�����*/
    int currentLoc = emitSkip(0);
    emitBackup(savedLoc);
    emitRM_Abs("JNE",ac2,currentLoc,"not jump");
    emitRestore();    
}
/************************************************/
/* ������ valactGen				*/ 
/* ��  �� �β�Ϊֵ��ʱ����ʵ�ν�ϴ�������	*/		
/* ˵  ��					*/
/************************************************/
void valactGen(CodeFile midcode)
{
    /* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,��ʵ�ν�Ͽ�ʼ*/
    if (TraceCode)  
        emitComment("->param  combine ");

    /*ȡ���βε�ƫ��*/
    int paramoff = midcode.codeR.arg2.midAttr.value;

    /*���ú������õ�ʵ�ε�ֵ,����ac��*/
    operandGen(midcode.codeR.arg1);
	
    /*������ʵ�ν�ϣ����µ�AR�Ķ�Ӧ�β�λ��д��ʵ��ֵ*/
    emitRM("ST",ac,paramoff,top,"store  param value");

    /* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,��ʵ�ν�Ͻ���*/
    if (TraceCode)  
        emitComment("<-param  combine ");
}
/************************************************/
/* ������ varactGen				*/ 
/* ��  �� �β�Ϊ���ʱ�Ĵ�������		*/		
/* ˵  ��					*/
/************************************************/
void varactGen(CodeFile midcode)
{
    /* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,��ʵ�ν�Ͽ�ʼ*/
    if (TraceCode)  
        emitComment("->param  combine ");
	
    /*��¼������ƫ��*/
    int paramoff = midcode.codeR.arg2.midAttr.value;
   
    /*�β��Ǳ��*/
    /*1.ʵ����ֱ�ӱ���*/
    if (midcode.codeR.arg1.midAttr.addr.access.equals("dir"))
    {   
        /*ac�еĵ�ַ��Ϊʵ�ε�ַ*/
	/*��������ľ���ƫ��,ac�д�Ϊ�����ľ���ƫ��*/
        FindAddr(midcode.codeR.arg1);
    }
    /*2.ʵ���Ǽ�ӱ���*/
    else
    {   
        /*ac�еĵ�ַ��ŵ�����Ϊʵ�ε�ַ*/
	/*��������ľ���ƫ��,ac�д�Ϊ�����ľ���ƫ��*/
        FindAddr(midcode.codeR.arg1);
	emitRM("LD",ac,0,ac,"");
    }
    
    /*������ʵ�ν�ϣ����µ�AR�Ķ�Ӧ�β�λ��д��ʵ�α����ĵ�ַ*/
    emitRM("ST",ac,paramoff,top,"store  param  var");

    /* �����������׷�ٱ�־TraceCodeΪTRUE,д��ע��,��ʵ�ν�Ͻ���*/
    if (TraceCode)  
        emitComment("<-param  combine ");	
}					
/************************************************/
/* ������ callGen				*/ 
/* ��  �� ���̵��ô��Ĵ�����			*/		
/* ˵  �� Ϊ�˽�ԼĿ����룬�����̵����е�����  */
/*	  �������䵽�����֣����̵��ô���������  */
/*	  �ڴ������̳��ڴ�			*/
/************************************************/
void callGen(CodeFile midcode)
{
    /*����ɵ�display���ƫ��*/
    emitRM("ST",displayOff,6,top," save nOff");
	
    /*�µ�displayOff��ֵ*/ 
    int Noff = midcode.codeR.arg3.midAttr.value;
    emitRM("LDC",displayOff,Noff,0," new displayOff");

    /*��¼�·��ص�ַ,ע�����ص�ַֻ����������ã��������������淵��
      ��ַ������ָ�����ת��һ��ָ�����һ��ָ��ʼ�3*/
    int currentLoc = emitSkip(0)+3;
  
    /*���淵�ص�ַ*/
    emitRM("LDC",ac,currentLoc,0,"save return address");
    emitRM("ST",ac,1,top,"");

    /*����תָ�����ͬ,ע�⣺��תд�����*/
    jumpGen(midcode,1);
}
/************************************************/
/* ������ pentryGen				*/ 
/* ��  �� ��������ڴ��Ĵ���			*/		
/* ˵  �� ��������м�����У�ARG1�ǹ������	*/
/*	  ��ţ�ARG2��diplay���ƫ������ARG3	*/
/*	  �ǹ��̵Ĳ���				*/
/************************************************/
void pentryGen(CodeFile midcode)
{
    /*���ȴ�����,���ñ�Ŵ�����*/
    labelGen(midcode);
    
    /*ȡ��AR��С��Ϣ*/
    int ARsize = midcode.codeR.arg2.midAttr.value;

    /*ȡ�����̲�����Ϣ*/
    int procLevel = midcode.codeR.arg3.midAttr.value;

    /*���浱ǰsp*/
    emitRM("ST",sp,0,top,"save old sp");

    /*����Ĵ���0,1,2,4*/
    emitRM("ST",ac,3,top,"save ac");
    emitRM("ST",ac1,4,top,"save ac1");
    emitRM("ST",ac2,5,top,"save ac2");
	
    /*������̲���*/
    emitRM("LDC",ac,procLevel,0,"save procedure level");
    emitRM("ST",ac,2,top,"");

    /*��display��*/
    for(int ss=0;ss<procLevel;ss++)
    {
	/*ȡԭdisplayOff,����ac1��*/
	emitRM("LD",ac1,6,top," fetch old display Off");
	/*ssҪ���ϵ�ǰnOff���Ƕ���sp��ƫ��*/
	emitRM("LDA",ac1,ss,ac1," old display item");
	/*ac1��Ϊ���Ե�ַ*/
	emitRO("ADD",ac1,ac1,sp,"");
	/*ȡ��ǰAR��display��ĵ�ss��,����ac��*/
	emitRM("LD",ac,0,ac1," fetch display table item");
				
	/*��ǰAR��displayOff*/
	emitRM("LDA",ac1,ss,displayOff," current display item");
	/*ac1��Ϊ���Ե�ַ*/
	emitRO("ADD",ac1,ac1,top,"");
	/*��ac�е���������ac1��ָ��ַ��*/
	emitRM("ST",ac,0,ac1," send display table item");
    }
    /*��display���е����ϲ���д�����sp*/
    /*ac�д洢��Ϊdisplay�����ϲ�����off*/
    emitRM("LDA",ac,procLevel,displayOff," current sp in display");
    emitRO("ADD",ac,top,ac," absolute off");
    emitRM("ST",top,0,ac," store address" );

    /*�޸�sp��top*/
    emitRM("LDA",sp,0,top," new sp value");
    emitRM("LDA",top,ARsize,top,"new top value");
}
/************************************************/
/* ������ endprocGen				*/ 
/* ��  �� ���̳��ڴ��Ĵ���			*/		
/* ˵  ��					*/
/************************************************/
void endprocGen(CodeFile midcode)
{
    /*�ָ��Ĵ���ֵ*/
    emitRM("LD",ac,3,sp,"resume ac");
    emitRM("LD",ac1,4,sp,"resume ac1");
    emitRM("LD",ac2,5,sp,"resume ac2");
    emitRM("LD",displayOff,6,sp,"resume nOff");

    /*�ָ�sp��topֵ*/
    emitRM("LDA",top,0,sp,"resume top");
    emitRM("LD",sp,0,sp,"resume sp");

    /*ȡ�����ص�ַ������*/		
    emitRM("LD",pc,1,top," load return address");
}
/***********************************************************/
/* ������  mentryGen                                       */
/* ��  ��  ��������ڵĴ�����			           */
/* ˵  ��  ����������м�����ARG2��¼��������AR��display */
/*	   ���ƫ�ƣ�����savedLoc��¼��ת�ı�pc��ָ��Ӧ�ڵ�*/
/*	   λ��						   */
/***********************************************************/
void mentryGen(CodeFile midcode,int savedLoc)
{
    /*���������*/
    int currentLoc = emitSkip(0); 
	
    /*���˵�Ŀ������һ������䴦*/
    emitBackup(savedLoc);
    /*���ָ�����������ڵ�ַ����ָ��Ĵ���pc*/
    emitRM("LDC",pc,currentLoc,0,"main entry");
    /*�ָ���ǰ��ַ*/
    emitRestore();

    emitComment("-> main procedure");
    /*����������Ĺ��̻��¼����Ҫ��д��������:ȫ�ֱ�����display��*/   
    /*��ʼ���Ĵ���*/
    emitRM("LDC",ac,0,0,"initialize ac");
    emitRM("LDC",ac1,0,0,"initialize ac1");
    emitRM("LDC",ac2,0,0,"initialize ac2");

    /*ȷ��sp*/
    emitRM("ST",ac,0,sp," main sp");

    /*ȷ��displayOff*/	
    int Noff = midcode.codeR.arg3.midAttr.value;
    emitRM("LDC",displayOff,Noff,0," new displayOff");

    /*��дdisplay��ֻ�������򱾲��sp(0)*/
    emitRM("ST",ac,0,displayOff," main display ");

    /*��дtop������AR�Ĵ�С*/
    int size = midcode.codeR.arg2.midAttr.value;
    emitRM("LDA",top, size, sp," main top");					
}
/***********************************************************/
/* ������  FindAddr                                        */
/* ��  ��  ��������ľ��Ե�ַ				   */
/* ˵  ��  �����Ե�ַ����ac��,ע��Ҫ��֤���õ�ac2	   */
/***********************************************************/
void FindAddr( ArgRecord arg)
{
    /*Դ��������ʱ��������ʽ��ͬ*/
    /*��¼�ñ������ڲ�*/
    int varLevel = arg.midAttr.addr.dataLevel;
    /*��¼�ñ��������ƫ��*/
    int varOff = arg.midAttr.addr.dataOff;
    /*Դ����*/
    if (varLevel != -1)
    {			
	/*����ñ�����sp������ac��*/
	FindSp(varLevel);
	/*�ñ��������sp��ƫ�ƣ�����ac1��*/
	emitRM("LDC",ac1,varOff,0," ");
	/* �������ƫ�� */
	emitRO("ADD",ac,ac,ac1," var absolute off");
    }
    /*��ʱ����*/
    else
    {  
        /*��ʱ�����Ǿֲ��ģ�ֻ�ڱ�AR����Ч*/
        /*�ñ��������sp��ƫ�ƣ�����ac1��*/
	emitRM("LDC",ac1,varOff,0," ");
	/* �������ƫ�� */
	emitRO("ADD",ac,sp,ac1," temp absolute off"); 
    }
}
/***********************************************************/
/* ������  FindSp                                          */
/* ��  ��  �ҵ��ñ�������AR��sp,����ac��                   */
/* ˵  ��						   */
/***********************************************************/
void FindSp(int varlevel)
{
    /*����ñ���������AR�е�λ�ã�����varLevel��ʾ�������ڲ�*/
    emitRM("LDA",ac,varlevel,displayOff," var process");
	
    /*���Ե�ַ*/
    emitRO("ADD",ac,ac,sp," var sp relative address");
    
    /*�ñ�������AR��sp��ַ����ac1��*/
    emitRM("LD",ac,0,ac," var sp");
}
/****************************************************************/
/* ������ emitComment						*/
/* ��  �� ע�����ɺ���						*/
/* ˵  �� �ú�������������cָ����ע������д������ļ�code	*/
/****************************************************************/
void emitComment(String c)
/* �����������׷�ٱ�־TraceCodeΪTRUE,��ע��д��Ŀ������ļ� */
{
    if (TraceCode) 
	mbcode=mbcode+"* "+c+"\n";
}
/********************************************************/
/* ������ emitRO					*/
/* ��  �� �Ĵ�����ַģʽָ�����ɺ���			*/
/* ˵  �� �ú�������һ��ֻ�üĴ�����������TMָ��	*/
/*	  op Ϊ������;				        */
/*	  r  ΪĿ��Ĵ���;				*/
/*	  s  ��һԴ�Ĵ���;				*/
/*	  t  �ڶ�Դ�Ĵ���;				*/
/*        c  Ϊ��д������ļ�code��ע������		*/
/********************************************************/
void emitRO(String op,int r,int s,int t,String c)
{  
             /* ��TMָ���ʽ��д������ļ�,��ǰ���ɴ���д���ַemitLoc��1 */
             mbcode=mbcode+String.valueOf(emitLoc++)+":"+op+"  "+String.valueOf(r)+","+String.valueOf(s)+","+String.valueOf(t);

             /* �����������׷�ٱ�־TraceCodeΪTRUE,��ע��cд������ļ� */
             if (TraceCode)    
	         mbcode=mbcode+"\t"+"*"+c;
             /* һ������ָ��д��,��������н�����־ */
             mbcode=mbcode+"\n";
    /* ��ǰ���ɴ���д���ַ������������ɴ���д���ַ						       �ı�������ɴ���д���ַhighEmitLocΪ��ǰ���ɴ���д���ַemitLoc */
    if (highEmitLoc<emitLoc) 
        highEmitLoc = emitLoc ;
} 
/********************************************************/
/* ������ emitRM					*/
/* ��  �� ��ַ��ַģʽָ�����ɺ���			*/
/* ˵  �� �ú�������һ���Ĵ���-�ڴ������TMָ��		*/
/*	  op ������;					*/
/*	  r  Ŀ��Ĵ���;				*/
/*	  d  Ϊƫ��ֵ;					*/
/*        s  Ϊ����ַ�Ĵ���;				*/
/*	  c  Ϊ��д������ļ�code��ע������		*/
/********************************************************/
void emitRM(String op,int r,int d,int s,String c)
{ 
             /* ��TMָ���ʽ��д������ļ�,��ǰ���ɴ���д���ַemitLoc��1 */
             mbcode=mbcode+String.valueOf(emitLoc++)+":"+op+"  "+String.valueOf(r)+","+String.valueOf(d)+"("+String.valueOf(s)+")";

             /* �����������׷�ٱ�־TraceCodeΪTRUE,��ע��cд������ļ� */
             if (TraceCode)    
	         mbcode=mbcode+"\t"+"*"+c;
             /* һ������ָ��д��,��������н�����־ */
             mbcode=mbcode+"\n";
    /* ��ǰ���ɴ���д���ַ������������ɴ���д���ַ						       �ı�������ɴ���д���ַhighEmitLocΪ��ǰ���ɴ���д���ַemitLoc */
    if (highEmitLoc<emitLoc) 
        highEmitLoc = emitLoc ;
} 
/****************************************************/
/* ������ emitSkip				    */	
/* ��  �� �չ����ɺ���				    */
/* ˵  �� �ú����չ�howManyָ��������д�����λ��,  */
/*	  ���ص�ǰ���ɴ���д���ַ	            */
/****************************************************/
int emitSkip(int howMany)
{  
    /* ��ǰ���ɴ���д���ַemitLoc��������i */
    int i = emitLoc;

    /* �µĵ�ǰ���ɴ���д���ַemitLoc�Թ�howManyָ��������д��ָ��λ�� */
    emitLoc = emitLoc+howMany ;

    /* ����ǰ���ɴ���д���ַemitLoc����������ɴ���д���ַhighEmitLoc	
       ����������ɴ���д���ַhighEmitLoc	*/
    if (highEmitLoc < emitLoc)  
        highEmitLoc = emitLoc ;

    /* �������ؾɵĵ�ǰ���ɴ���д���ַi */
    return i;
} 
/********************************************************/
/* ������ emitBackup					*/
/* ��  �� ��ַ���˺���					*/	
/* ˵  �� �ú����˻ص���ǰ���չ������ɴ���д���ַloc	*/
/********************************************************/
void emitBackup(int loc)
{
   /* ���Ҫ�˻صĵ�ַloc�ȵ�ǰ��ߵ�ַhighEmitLoc����	
      ���˻ش���,��������Ϣ��Ϊע��д������ļ�code	*/
   if (loc > highEmitLoc) 
       emitComment("BUG in emitBackup");

   /* ���µ�ǰ���ɴ���д���ַemitLocΪ��������loc,����˻ض��� */
   emitLoc = loc ;
} 
/********************************************************/
/* ������ emitRestore					*/
/* ��  �� ��ַ�ָ�����					*/
/* ˵  �� �ú�������ǰ���ɴ���д���ַemitLoc�ָ�Ϊ	*/
/*	  ��ǰδд��ָ�����ߵ�ַhighEmitLoc	        */
/********************************************************/
void emitRestore()
{ 
    emitLoc = highEmitLoc;
}
/************************************************/
/* ������ emitRM_Abs				*/
/* ��  �� ��ַת������				*/
/* ˵  �� �ú����ڲ���һ���Ĵ���-�ڴ�TMָ��ʱ,	*/
/*	  �����Ե�ַ����ת����pc��Ե�ַ����	*/
/*	  op Ϊ������;				*/
/*        r  ΪĿ��Ĵ���;			*/
/*	  a  Ϊ�洢�����Ե�ַ;			*/
/*	  c  Ϊ��д������ļ�code��ע��		*/
/************************************************/
void emitRM_Abs(String op,int r,int a,String c)
{  
             /* ��TMָ���ʽ��д������ļ�,����������a�����ľ��Ե�ַ
                ת��Ϊ�����ָ��ָʾ��pc����Ե�ַa-(emitLoc+1) */
             mbcode=mbcode+String.valueOf(emitLoc)+":"+op+"  "+String.valueOf(r)+","+String.valueOf(a-(emitLoc+1))+"("+String.valueOf(pc)+")";

             /* ���µ�ǰ���ɴ���д���ַemitLoc */
             ++emitLoc;

             /* �����������׷�ٱ�־TraceCodeΪTRUE,��ע��cд������ļ� */
             if (TraceCode)    
	         mbcode=mbcode+"\t"+"*"+c;
             /* һ������ָ��д��,��������н�����־ */
             mbcode=mbcode+"\n";
    /* ��ǰ���ɴ���д���ַ������������ɴ���д���ַ						       �ı�������ɴ���д���ַhighEmitLocΪ��ǰ���ɴ���д���ַemitLoc */
    if (highEmitLoc<emitLoc) 
        highEmitLoc = emitLoc;
} 
}

/********************************************************************/
/* ��  �� Opt	                                                    */
/* ��  �� �ܳ���Ĵ���					            */
/* ˵  �� ����һ���࣬�����ܳ���                                    */
/********************************************************************/
class Opt
{
CodeFile baseBlock[]=new CodeFile[100];
int blocknum;

/*��ʱ������ţ�ȫ�ֱ���,ÿ�����̿�ʼ����TempOffset����
  ��ʼ����ע�����ԣ���ͬ�����У������б����ͬ����ʱ����������
  �������ǻ�����ɣ����Բ��������⣻�����Ż������ǶԻ���������Ż���
  ÿ�������������һ�����̣�Ҳ���������� */
int TempOffset=0;

/*���ֵ��ȫ�ֱ���*/
int Label=0;

/*ָ���һ���м����*/
CodeFile firstCode;

/*ָ��ǰ���һ���м����*/
CodeFile lastCode;

/*��¼������display���ƫ����*/
int StoreNoff;

/*������ֵ��*/
ConstDefT table;

ValuNum valuNumT;
UsableExpr usableExprT;
TempEqua tempEquaT;
/*��¼ֵ����*/
int Vnumber=0;

/*������ֵ��,�ñ�����arg�ṹ��ʾ����*/
ArgRecord varTable[] = new ArgRecord[100];
int TotalNum = 0;

/*ѭ����Ϣջ*/
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
/* ������  GenMidCode 	  				*/
/* ��  ��  �м��������������				*/
/* ˵  ��  ���й������������ù��������Ĵ�������������   */
/*         ���ó�����Ĵ������ɺ���			*/
/********************************************************/
CodeFile GenMidCode(TreeNode t)
{  
    /*���й���������������Ӧ���������������������м����*/
    TreeNode t1=t.child[1];
    while (t1!=null)
    { 
        if (t1.nodekind.equals("ProcDecK"))
            GenProcDec(t1);
        t1=t1.sibling; 
    }
  
    /*display�������sp��ƫ��*/
    ArgRecord Noff = ARGValue(StoreNoff);

    /*���������������������*/
    CodeFile code = GenCode("MENTRY",null,null,Noff);

    /*��ʼ����ʱ�����Ŀ�ʼ���,Ϊ��ʱ�������ĵ�һ����ַ*/
    TempOffset = StoreNoff + 1;

    /*����������еĴ������ɺ���*/
    GenBody(t.child[2]);

    /*�����������AR�Ĵ�С������������м����*/
    int size = TempOffset;
    ArgRecord sizeArg = ARGValue(size);
    code.codeR.arg2= sizeArg;

    return firstCode;
}
/****************************************************/
/* ������  GenProcDec				    */
/* ��  ��  ���������м�������ɺ���		    */
/* ˵  ��  ���ɹ�������м���룬���ɹ�������м�   */
/*	   ���룬���ɹ��̳��ڵ��м����		    */	
/****************************************************/
void GenProcDec(TreeNode t)
{  
    /*�õ����̵���ڱ��*/
    int ProcEntry = NewLabel();
  
    /*�������ڷ��ű��еĵ�ַ*/
    SymbTable Entry = t.table[0];
    /*������ڱ�ţ�������ڵ���*/
    Entry.attrIR.proc.codeEntry = ProcEntry;

    /*���̵�display���ƫ����*/
    int noff = Entry.attrIR.proc.nOff;
 
    /*�õ����̵Ĳ�������ARG�ṹ*/
    int procLevel = Entry.attrIR.proc.level;
    ArgRecord levelArg = ARGValue(procLevel);
  
    /*�������ڲ����й���������������Ӧ���������������������м����*/
    TreeNode t1=t.child[1];
    while (t1!=null)
    { 
        if (t1.nodekind.equals("ProcDecK"))
            GenProcDec(t1);
        t1=t1.sibling; 
    }

    /*������������м����*/ 
    ArgRecord arg1 = ARGLabel(ProcEntry);
    CodeFile code = GenCode("PENTRY",arg1,null,levelArg);
  
    /*��ʼ����ʱ�����Ŀ�ʼ���,Ϊ������ʱ�������ĵ�һ����ַ*/
    TempOffset =  noff + procLevel+1;

    /*����������еĴ������ɺ������������*/
    GenBody(t.child[2]);

    /*�õ����̵�AR�Ĵ�С,�������������м����*/
    int size = TempOffset;
    ArgRecord sizeArg = ARGValue(size);
    code.codeR.arg2 = sizeArg;

    /*�������̳����м����*/
    GenCode("ENDPROC",null,null,null);
}
/****************************************************/
/* ������  GenBody				    */
/* ��  ��  ��������м�������ɺ���		    */
/* ˵  ��  ���ڴ����������߳����壬		    */
/*	   ѭ������������			    */	
/****************************************************/
void GenBody(TreeNode t)
{  
    TreeNode t1 = t;
    /*��ָ��ָ���һ�����*/
    if (t1.nodekind.equals("StmLK"))
	t1=t1.child[0];

   while (t1!=null)
   { 
       /*������䴦����*/
       GenStatement(t1);
       t1= t1.sibling;
   }
}
/****************************************************/
/* ������  GenStatement				    */
/* ��  ��  ��䴦����	        		    */
/* ˵  ��  �������ľ������ͣ��ֱ������Ӧ��	    */
/*	   ��䴦����				    */
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
    else if (t.kind.equals("ReturnK"))    /*ֱ�������м����*/  	
        GenCode("RETURNC",null,null,null);
}
/****************************************************/
/* ������  GenAssignS				    */
/* ��  ��  ��ֵ��䴦����        	            */
/* ˵  ��  �����󲿱����������Ҳ����ʽ������       */
/*         ��ֵ����м���� 			    */
/****************************************************/
void GenAssignS(TreeNode t)
{   
    /*���ø�ֵ�󲿱����Ĵ�����*/
    ArgRecord Larg = GenVar(t.child[0]);
    /*���ø�ֵ�Ҳ����ʽ�Ĵ�����*/
    ArgRecord Rarg = GenExpr(t.child[1]);
    /*���ɸ�ֵ����м����*/
    GenCode("ASSIG",Rarg,Larg,null);
}
/****************************************************/
/* ������  GenVar				    */
/* ��  ��  ����������        		            */
/* ˵  ��					    */
/****************************************************/
ArgRecord GenVar(TreeNode t)
{ 
    int low,size;
    FieldChain head;

    /*���ɱ�������ARG�ṹ, EntryΪ��ʶ���ڷ��ű��еĵ�ַ*/
    SymbTable Entry = t.table[0];
    ArgRecord V1arg = ARGAddr(t.name[0],Entry.attrIR.var.level,
Entry.attrIR.var.off,Entry.attrIR.var.access);
    
    /*���ص�ARG�ṹ*/
    ArgRecord Varg=null;
    if (t.attr.expAttr.varkind.equals("IdV"))
        /*��ʶ����������*/
 	Varg = V1arg; 
    else if (t.attr.expAttr.varkind.equals("ArrayMembV"))
    {    
	/*�����Ա��������*/
	/*���������½�������С��ARG�ṹ*/
	low = Entry.attrIR.idtype.array.low;
	size = Entry.attrIR.idtype.array.elementTy.size;
        Varg = GenArray(V1arg,t,low,size);
    }
    else if (t.attr.expAttr.varkind.equals("FieldMembV"))
    {
        /*���������*/    
	head = Entry.attrIR.idtype.body;
	Varg = GenField(V1arg,t,head);
    }
    return Varg;
}
/****************************************************/
/* ������  GenArray				    */
/* ��  ��  �����Ա����������    		    */
/* ˵  ��  �ɺ���GenVar����GenField����	    */
/****************************************************/      	  
ArgRecord GenArray(ArgRecord V1arg,TreeNode t,int low,int size)  		  
{   
    /*�����±���ʽ*/
    ArgRecord Earg= GenExpr(t.child[0]);

    ArgRecord lowArg = ARGValue(low);
    ArgRecord sizeArg= ARGValue(size);
    /*����������ʱ����*/
    ArgRecord temp1= NewTemp("dir");
    ArgRecord temp2= NewTemp("dir");
    /*ע����ʾ���ӱ�������ʱ�������ڼ�ӷ���*/
    ArgRecord temp3= NewTemp("indir"); 
	      
    /*�����м����*/
    GenCode("SUB", Earg, lowArg ,temp1);
    GenCode("MULT",temp1,sizeArg,temp2);
    GenCode("AADD",V1arg,temp2, temp3);

    return temp3;
}		  		  		  
/****************************************************/
/* ������  GenField				    */
/* ��  ��  �����������    			    */
/* ˵  ��  �ɺ���GenVar����			    */
/****************************************************/		  
ArgRecord GenField(ArgRecord V1arg,TreeNode t,FieldChain head)		  
{   
    ArgRecord FieldV;
    /*t1ָ��ǰ���Ա*/
    TreeNode t1 = t.child[0];
    FieldChain Entry2=new FieldChain();

    FindField(t1.name[0],head,Entry2);
    /*����������е�ƫ��*/
    int off = Entry2.off;
    ArgRecord offArg = ARGValue(off);
    /*ע����ʾ���ӱ�������ʱ�������ڼ�ӷ���*/
    ArgRecord temp1 = NewTemp("indir");
    GenCode("AADD",V1arg,offArg,temp1);
    /*�����������*/
    if (t1.attr.expAttr.varkind.equals("ArrayMembV"))
    {  
        int low = Entry2.unitType.array.low;
 	int size= Entry2.unitType.array.elementTy.size;
	FieldV = GenArray(temp1,t1,low,size);
    }
    else  /*���Ǳ�ʶ������*/
   	FieldV = temp1;

    return FieldV;
}
/****************************************************/
/* ������  GenExpr				    */
/* ��  ��  ���ʽ������        		    */
/* ˵  ��					    */
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
	/*�õ�ֵ��ARG�ṹ*/
	arg = ARGValue(t.attr.expAttr.val);
    else if (t.kind.equals("OpK"))
    {
	/*�����󲿺��Ҳ�*/
	Larg = GenExpr(t.child[0]);
	Rarg = GenExpr(t.child[1]);

	/*���ݲ�������𣬵õ��м��������*/
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
	/*����һ���µ���ʱ����*/	
        temp = NewTemp("dir");
	/*�����м����*/
	GenCode(op,Larg,Rarg,temp);
	arg = temp ;
    }
    return arg;
}
/****************************************************/
/* ������  GenCall				    */
/* ��  ��  ���̵��ô�����        		    */
/* ˵  ��  �ֱ���ñ��ʽ�������������ʵ�Σ���   */
/*	   ������Ӧ����ʵ�ν���м���룻�ӷ��ű��� */
/*	   ���̱�ʶ�������У��鵽��ڱ�ţ��������� */
/*	   �����м����				    */
/****************************************************/
void GenCallS(TreeNode t)
{
    /*ȡ�ù��̱�־���ڷ��ű��еĵ�ַ*/
    SymbTable Entry = t.child[0].table[0];
    ParamTable param = Entry.attrIR.proc.param;
    /*���ñ��ʽ�������������ʵ�Σ�
      ��������Ӧ����ʵ�ν���м����*/
    TreeNode t1 = t.child[1];
    ArgRecord Earg;
    while (t1!=null)
    { 
        Earg = GenExpr(t1);

        /*��¼������ƫ��*/
        int paramOff = param.entry.attrIR.var.off;
	ArgRecord OffArg = ARGValue(paramOff);
        /*��ʵ�ν���м����*/
	if (param.entry.attrIR.var.access.equals("dir")) 
	    /*ֵ�ν���м����*/
            GenCode("VALACT",Earg,OffArg,null);
	else  /*��ν���м����*/
	    GenCode("VARACT",Earg,OffArg,null);
	
	t1 = t1.sibling;
        param = param.next;
    } 
    /*������ڱ�ż���ARG�ṹ*/
    int label = Entry.attrIR.proc.codeEntry;
    ArgRecord labelarg = ARGLabel(label);
   
    /*���̵�display���ƫ����*/
    int Noff = Entry.attrIR.proc.nOff;
    ArgRecord Noffarg = ARGValue(Noff);

    /*���ɹ��̵����м����*/
    GenCode ("CALL",labelarg,null,Noffarg);
}
/****************************************************/
/* ������  GenReadS				    */
/* ��  ��  ����䴦����        		    */
/* ˵  ��  �õ����������ARG�ṹ�����ɶ�����м����*/
/****************************************************/
void GenReadS(TreeNode t)
{ 
    SymbTable Entry = t.table[0];
    ArgRecord Varg = ARGAddr(t.name[0],Entry.attrIR.var.level,
Entry.attrIR.var.off,Entry.attrIR.var.access);
    /*���ɶ�����м����*/
    GenCode("READC",Varg,null,null);
}
/****************************************************/
/* ������  GenWrite				    */
/* ��  ��  д��䴦����        		    */
/* ˵  ��  ���ñ��ʽ���м�������ɺ�����������д   */
/*	   �����м����			    */
/****************************************************/
void GenWriteS(TreeNode t)
{   
    /*���ñ��ʽ�Ĵ���*/
    ArgRecord Earg = GenExpr(t.child[0]);
    /*����д����м����*/
    GenCode("WRITEC",Earg,null,null);
}
/****************************************************/
/* ������  GenIfs				    */
/* ��  ��  ������䴦����        	            */
/* ˵  ��					    */
/****************************************************/
void GenIfS(TreeNode t)
{   
    /*����else������ڱ�ţ�����ARG�ṹ*/
    int elseL = NewLabel();
    ArgRecord ElseLarg=ARGLabel(elseL);

    /*����if�����ڱ�ţ�����ARG�ṹ*/
    int outL = NewLabel();
    ArgRecord OutLarg = ARGLabel(outL);

    /*�������ʽ���м��������*/
    ArgRecord Earg = GenExpr(t.child[0]);

    /*�����ʽΪ�٣���ת��else��ڱ��*/
    GenCode("JUMP0",Earg,ElseLarg,null);
    
    /*then�����м��������*/
    GenBody(t.child[1]);
    
    /*����if����*/
    GenCode("JUMP",OutLarg,null,null);

    /*else������ڱ������*/
    GenCode("LABEL",ElseLarg,null,null);

    /*else�����м��������*/
    GenBody(t.child[2]);

    /*if�����ڱ������*/
    GenCode("LABEL",OutLarg,null,null);
}
/****************************************************/
/* ������  GenWhileS				    */
/* ��  ��  ѭ����䴦����        		    */
/* ˵  ��  ��ѭ����ںͳ����ò�ͬ���м�����־���� */
/*	   Ϊ��ѭ������ʽ�������Ҫ		    */
/****************************************************/
void GenWhileS(TreeNode t)
{   
    /*����while�����ڱ�ţ�����ARG�ṹ*/
    int inL = NewLabel() ;
    ArgRecord InLarg = ARGLabel(inL);

    /*����while�����ڱ�ţ�����ARG�ṹ*/
    int outL = NewLabel();
    ArgRecord OutLarg = ARGLabel(outL);

    /*while�����ڱ������*/
    GenCode("WHILESTART",InLarg,null,null);
    
    /*�������ʽ���м��������*/
    ArgRecord Earg = GenExpr(t.child[0]);

    /*�����ʽΪ�٣���ת��while������*/
    GenCode("JUMP0",Earg,OutLarg,null);
    
    /*ѭ�����м��������*/
    GenBody(t.child[1]);
    
    /*����while���*/
    GenCode("JUMP",InLarg,null,null);

    /*while���ڱ������*/
    GenCode("ENDWHILE",OutLarg,null,null);
}
/********************************************************/
/* ������  NewTemp		  			*/
/* ��  ��  ����һ���µ���ʱ������ARG�ṹ		*/
/* ˵  ��  ��ʱ�����Ĳ���Ϊ-1��ƫ��Ϊ���ֵ�����ʷ�ʽ�� */
/*	   ����ȷ��					*/
/********************************************************/
ArgRecord NewTemp(String access)
{  
    ArgRecord newTemp=new ArgRecord();
    /*��д��ʱ������ARG����*/
   
    newTemp.form="AddrForm";
    newTemp.midAttr.addr=new Addr();
    newTemp.midAttr.addr.dataLevel=-1 ;
    newTemp.midAttr.addr.dataOff=TempOffset ;
    newTemp.midAttr.addr.access=access;
    /*��ʱ������ż�1*/   
    TempOffset++;
      
    return newTemp;
}
/********************************************************/
/* ������  NewLabel		  			*/
/* ��  ��  ����һ���µı��ֵ				*/
/* ˵  ��  ͨ��ȫ�ֱ���Label��1�������µı��ֵ		*/
/********************************************************/
int NewLabel()
{  
    Label++;  
    return Label;
}
/********************************************************/
/* ������  ARGAddr		  			*/
/* ��  ��  ���ڸ����ı���������Ӧ��ARG�ṹ		*/
/* ˵  ��  						*/
/********************************************************/
ArgRecord ARGAddr(String id,int level,int off,String access)
{   
    ArgRecord arg = new ArgRecord();
    /*��д����ARG�ṹ������*/
    arg.form = "AddrForm";
    arg.midAttr.addr=new Addr();
    arg.midAttr.addr.name=id;
    arg.midAttr.addr.dataLevel=level;
    arg.midAttr.addr.dataOff=off;
    arg.midAttr.addr.access=access;
		  
    return arg;
}
/********************************************************/
/* ������  ARGLabel		  			*/
/* ��  ��  ���ڸ����ı�Ų�����Ӧ��ARG�ṹ		*/
/* ˵  ��  						*/
/********************************************************/
ArgRecord ARGLabel(int label)
{  
    ArgRecord arg = new ArgRecord();
    arg.form = "LabelForm";
    arg.midAttr.label = label;

    return arg;
}
/********************************************************/
/* ������  ARGValue		  			*/
/* ��  ��  ���ڸ����ĳ���ֵ������Ӧ��ARG�ṹ	        */
/* ˵  ��  						*/
/********************************************************/
ArgRecord ARGValue(int value)
{ 
    ArgRecord arg = new ArgRecord();
    arg.form = "ValueForm";
    arg.midAttr.value = value;

    return arg;
}
/********************************************************/
/* ������  GenCode 		  			*/
/* ��  ��  ���ݸ�������������һ���м����		*/
/* ˵  ��						*/
/********************************************************/
CodeFile GenCode(String codekind,ArgRecord Arg1,ArgRecord Arg2,ArgRecord Arg3)
{ 
    CodeFile newCode = new CodeFile();
    /*��д���������*/	
    newCode.codeR.codekind = codekind;
    newCode.codeR.arg1 = Arg1;  
    newCode.codeR.arg2 = Arg2;
    newCode.codeR.arg3 = Arg3;
    /*�����м�������*/
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
/* ������  FindField	  				*/
/* ��  ��  ���Ҽ�¼������				*/
/* ˵  ��  ����ֵΪ�Ƿ��ҵ���־������Entry���ش�������  */
/*	   ��¼������е�λ��.			        */
/********************************************************/
boolean FindField(String Id,FieldChain head,FieldChain Entry)
{ 
    boolean  present=false;
    /*��¼��ǰ�ڵ�*/
    FieldChain currentItem = head;
    /*�ӱ�ͷ��ʼ���������ʶ����ֱ���ҵ��򵽴��β*/
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
/* ������  ConstOptimize			    */
/* ��  ��  �����ʽ�Ż�������      		    */
/* ˵  ��  ѭ���Ը�����������г����ʽ�Ż�	    */
/****************************************************/
CodeFile ConstOptimize()
{    
    /*���û��ֻ����麯��*/
    blocknum = DivBaseBlock();

    /*ѭ����ÿ����������г����ʽ�Ż�*/
    for (int i=0;i<blocknum;i++)
    {  
        /*��������ڴ��ó�����ֵ��Ϊ��*/
	table = null;
	/*������ĳ����ʽ�Ż�*/
        OptiBlock(i);
    }
    /*�����Ż�����м����*/
    return firstCode;
}
/****************************************************/
/* ������  OptiBlock				    */
/* ��  ��  ��һ����������г����ʽ�Ż�		    */
/* ˵  ��					    */
/****************************************************/
void OptiBlock(int i)
{   
    boolean delCode;
    /*ָ��������һ�����*/
    CodeFile currentCode = baseBlock[i] ;
    CodeFile formerCode;
    CodeFile laterCode;
	    
    ArgRecord arg1;
    ArgRecord arg2;

    /*ѭ������ÿ�����룬ֱ����ǰ���������*/
    while ((currentCode!=baseBlock[i+1])&&(currentCode!=null))
    {   
        if ((currentCode.codeR.codekind.equals("ADD"))||(currentCode.codeR.codekind.equals("SUB"))||(currentCode.codeR.codekind.equals("MULT"))||(currentCode.codeR.codekind.equals("DIV"))||(currentCode.codeR.codekind.equals("LTC"))||(currentCode.codeR.codekind.equals("EQC")))
	{ 
            /*�����͹�ϵ����*/ 
	    /*���������͹�ϵ���㴦����*/ 
	    delCode = ArithC(currentCode);
	    /*ɾ����ʶΪ��ʱ��ɾ����ǰ��Ԫʽ*/
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
	    /*�Ե�һ��ARG�ṹ����ֵ�滻*/
	    SubstiArg(currentCode,1);
	    arg1 = currentCode.codeR.arg1;
	    arg2 = currentCode.codeR.arg2;
	    /*���ǳ����ṹ���򽫳�����ֵ���볣����ֵ��*/
	    if (arg1.form.equals("ValueForm"))
		AppendTable(arg2,arg1.midAttr.value);
	    else  /*ɾ�����к��д˱����Ķ�ֵ*/ 
		DelConst(arg2);
	}
        else if (currentCode.codeR.codekind.equals("JUMP0")||currentCode.codeR.codekind.equals("WRITEC"))
	    /*�Ե�һ��ARG�ṹ����ֵ�滻*/
	    SubstiArg(currentCode,1);
        else if (currentCode.codeR.codekind.equals("AADD"))
	    /*�Եڶ���ARG�ṹ����ֵ�滻*/
	    SubstiArg(currentCode,2);

	/*��ָ��ָ����һ������*/
	currentCode = currentCode.next;
    }
}
/****************************************************/
/* ������  ArithC				    */
/* ��  ��  �������������͹�ϵ�Ƚϲ���		    */
/* ˵  ��  ���������1���������2����ֵ�滻�������� */
/*	   �����������д�볣����ֵ��������Ԫʽ   */
/*	   ɾ����־Ϊ��				    */
/****************************************************/
boolean ArithC(CodeFile code)
{
    boolean delCode = false;
    int value1,value2,result=0;
    /*�Է���1����ֵ�滻*/
    SubstiArg(code,1);
    ArgRecord arg1 = code.codeR.arg1;

    /*�Է���2����ֵ�滻*/
    SubstiArg(code,2);
    ArgRecord arg2 = code.codeR.arg2;

    String codekind =code.codeR.codekind;
    ArgRecord arg3 = code.codeR.arg3;

    /*�����������ǳ���*/
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
	/*�������д�볣����ֵ��*/
	AppendTable(arg3,result);
	/*��ǰ��ԪʽӦɾ��*/
        delCode = true;
    }
    return delCode;
}
/****************************************************/
/* ������  SubstiArg				    */
/* ��  ��  ��һ��ARG�ṹ����ֵ�滻		    */
/* ˵  ��  ����iָ�����м������ĸ�ARG�ṹ�����滻 */
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
    /*��ARG�ṹ�ǵ�ַ�࣬�ҳ�����ֵ�����ж�ֵ����ֵ�滻*/
    if (arg.form.equals("AddrForm"))
    {  
	boolean constflag = FindConstT(arg,Entry);
	if (constflag)
	{ 
            /*����һ��ֵ��ARG�ṹ���滻ԭ�е�ARG�ṹ*/
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
/* ������  FindConstT				    */
/* ��  ��  �ڳ�����ֵ���в��ҵ�ǰ�����Ƿ��ж�ֵ	    */
/* ˵  ��  ����Ϊ������ARG�ṹ�����ݱ�������ʱ����  */
/*	   ����һ���ʶ���������ֱ���		    */
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
            /*����һ��ֵ������ֱ��дΪEntry=t*/
            Entry.constValue = t.constValue;
	}
	t = t.next;
    }
    return present;
}
/****************************************************/
/* ������  AppendTable				    */
/* ��  ��  ���������䳣��ֵд�볣����ֵ��	    */
/* ˵  ��  ����һ���µĽڵ㣬��д������ֵ���ݣ����� */
/*	   �����				    */
/****************************************************/
void AppendTable(ArgRecord arg,int result)
{ 
    ConstDefT last = table;
    ConstDefT current = table;
    ConstDefT Entry = new ConstDefT();
    /*���ң����Ѵ��ڴ˱�������ı���ֵ*/
    boolean present =  FindConstT(arg,Entry);
    if (present)
	Entry.constValue = result;
    else
    {	
        /*���򣬴���һ���µĽڵ�*/
	ConstDefT newConst = new ConstDefT();
	newConst.constValue = result;
        newConst.variable = arg;

	/*��ǰ�ڵ���볣����ֵ����*/
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
/* ������  DelConst				    */
/* ��  ��  ɾ��һ��������ֵ			    */
/* ˵  ��  �����ڣ���ӳ�����ֵ����ɾ�����������   */
/****************************************************/
void DelConst(ArgRecord arg)
{   
    ConstDefT Entry = new ConstDefT();
    ConstDefT former;
    ConstDefT later;
    /*���ұ���,��������ɾ�������򣬽���*/
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
/* ������  DivBaseBlock					*/
/* ��  ��  Ϊ�м���뻮�ֻ�����				*/
/* ˵  ��  �������0��ʼ��ţ����б�δ��ݣ�����Ӧ����  */
/*	   ������Ϊ��ǰ������Ľ���			*/
/********************************************************/
int DivBaseBlock()
{   
    /*��ʼ����������Ŀ*/
    int blocknum = 0;	
    CodeFile code = firstCode;

    while (code!=null)
    {
	if ((code.codeR.codekind.equals("LABEL"))||(code.codeR.codekind.equals("WHILESTART"))||(code.codeR.codekind.equals("PENTRY"))||(code.codeR.codekind.equals("MENTRY")))
        {
	    /*����һ���µĻ�����*/
            baseBlock[blocknum] =code;
            blocknum++;
        } 
	else if ((code.codeR.codekind.equals("JUMP"))||(code.codeR.codekind.equals("JUMP0"))||(code.codeR.codekind.equals("RETURNC"))||(code.codeR.codekind.equals("ENDPROC"))||(code.codeR.codekind.equals("ENDWHILE")))
        {
	    /*����һ����俪ʼ������һ���µĻ�����*/
	    if (code.next!=null)
	    { 
                code = code.next;
		baseBlock[blocknum] =code;
                blocknum++;
	    }
	}
	else if (code.codeR.codekind.equals("VARACT"))
	{ 
	    /*�ҵ���Ӧ�Ĺ��̵�����䣬��Ϊ��������Ľ���*/
	    code = code.next;
	    while (!(code.codeR.codekind.equals("CALL")))
		code = code.next;
	    /*����һ����俪ʼ������һ���µĻ�����*/
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
/* ������  ECCsave				    */
/* ��  ��  �������ʽ�Ż�������    		    */
/* ˵  ��  ѭ���Ը�����������й������ʽ�Ż�	    */
/****************************************************/
/*ע�����ǽ�blocknum�����ֲ�������ȫ�ֱ�����װ����*/
CodeFile ECCsave()
{ 
    /*ѭ����ÿ����������й������ʽ�Ż�*/
    for (int i=0 ;i<blocknum;i++)
    {  
        /*��������ڴ���ֵ�����
	  ���ñ��ʽ����ʱ�����ȼ۱�Ϊ��*/
	valuNumT = null;
	usableExprT = null;
	tempEquaT = null;

	/*�������ECC��ʡ*/
        SaveInBlock(i);
    }
    /*�����Ż�����м����*/
    return firstCode;
}
/****************************************************/
/* ������  SaveInBlock				    */
/* ��  ��  �������Ż�����	    	            */
/* ˵  ��					    */
/****************************************************/
void SaveInBlock(int i)
{   
    int op1,op2,op3;
	
    /*ָ��������һ�����*/
    CodeFile currentCode = baseBlock[i];
    CodeFile formerCode = null;
    CodeFile laterCode = null;

    /* ѭ������������еĸ������*/
    while ((currentCode!=baseBlock[i+1])&&(currentCode!=null))
    {
        CodeFile substiCode = new CodeFile();
	/*���еȼ��滻*/
	EquaSubsti(currentCode);

	if ((currentCode.codeR.codekind.equals("ADD"))||(currentCode.codeR.codekind.equals("SUB"))||(currentCode.codeR.codekind.equals("MULT"))||(currentCode.codeR.codekind.equals("DIV"))||(currentCode.codeR.codekind.equals("LTC"))||(currentCode.codeR.codekind.equals("EQC"))||(currentCode.codeR.codekind.equals("AADD")))
	{ 
	    /*���ú���Process�������1�����ط���1�ı���*/
	    op1 = Process(currentCode,1);
	    /*���ú���Process�������2�����ط���2�ı���*/
	    op2 = Process(currentCode,2);

	    /*���ҿ��ñ��ʽ�����*/
	    FindECC(currentCode.codeR.codekind,op1,op2,substiCode);
	    /*���ҵ�����ǰ����ɽ�ʡ*/
	    if (substiCode.codeR.arg3!=null)
	    { 
                /*����ʱ�����ȼ۱������һ��*/
		AppendTempEqua(currentCode.codeR.arg3,substiCode.codeR.arg3);
		/*ɾ����ǰ����*/
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
	    else  /*û�ҵ������벻�ɽ�ʡ*/
	    { 
	        /*Ϊ�����������һ���µı��룬����ֵ�����*/
		Vnumber++;
		op3 = Vnumber;
		AppendValuNum(currentCode.codeR.arg3,op3);
		/*�����Ӧ��ӳ����*/
		MirrorCode mirror = GenMirror(op1,op2,op3);
		/*��ǰ����д����ñ��ʽ�����*/
	        AppendUsExpr(currentCode,mirror);
	    }
        }
	else if (currentCode.codeR.codekind.equals("ASSIG"))
        {
	    /*Process��������ֵ�Ҳ������ر���*/
	    op1 = Process(currentCode,1);
				
	    /*���Ǽ����ʱ������op1�ǵ�ַ�룻������ֵ��*/
	    op2 = op1;
				
	    /*�滻������и�ֵ�󲿵�ֵ����*/
	    SubstiVcode(currentCode.codeR.arg2,op2);

	    /*ɾ�����ñ��ʽ��������õ���ֵ��ֵ�������*/
	    DelUsExpr(currentCode.codeR.arg2);
	}
	/*������һ������*/
	currentCode = currentCode.next;
    }
}
/****************************************************/
/* ������  EquaSubsti				    */
/* ��  ��  ������ʱ�����ȼ۱�Ե�ǰ������еȼ��滻 */
/* ˵  ��  					    */
/****************************************************/
void EquaSubsti(CodeFile code)
{	
    TempEqua Entry = new TempEqua();
    if (code.codeR.arg1!=null)
	/*��������1����ʱ����,�Ҵ�������ʱ�����ȼ۱��У����滻*/
	if (code.codeR.arg1.form.equals("AddrForm"))
	    if(code.codeR.arg1.midAttr.addr.dataLevel == -1)
	    {	
                FindTempEqua(code.codeR.arg1,Entry);
		if (Entry.arg2!=null)
		    code.codeR.arg1 = Entry.arg2;
	    }
    if (code.codeR.arg2!=null)
	/*��������2����ʱ����,�Ҵ�������ʱ�����ȼ۱��У����滻*/
	if (code.codeR.arg2.form.equals("AddrForm"))
	    if (code.codeR.arg2.midAttr.addr.dataLevel == -1)
	    {	
                FindTempEqua(code.codeR.arg2,Entry);
		if (Entry.arg2!=null)
		    code.codeR.arg2 = Entry.arg2;
	    }
}
/****************************************************/
/* ������  Process				    */
/* ��  ��  ������������������ض�Ӧ�ı���	    */
/* ˵  ��  ���״γ��֣�������±��룬���������У� */
/*	   ����ֵȡ����µı��룻���򣬸����Ƿ��� */
/*	   ������������Ӧ��ֵ������ַ��	    */	
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
    /*���������״γ��֣�����������*/
    SearchValuNum(arg,Entry);
    if (Entry.access==null)
    { 
        Vnumber++;
	opC = Vnumber;  /*op1��¼������1��ֵ����*/
	AppendValuNum(arg,opC);
    }
    else 
    {  
	/*�����ʱ����*/
        if (Entry.access.equals("indir"))
	    /*�ü����ʱ�����ĵ�ַ��*/
	    if((codekind.equals("AADD"))||(codekind.equals("ASSIG")))
		/*ȡ��ַ��*/
		opC= Entry.codeInfo.twoCode.addrcode;
	    else   /*����ȡֵ��*/
		opC = Entry.codeInfo.twoCode.valuecode;
	/*�Ǽ����ʱ����*/
	else    
            opC = Entry.codeInfo.valueCode;
    }
    return opC;
}
/****************************************************/
/* ������  FindTempEqua				    */
/* ��  ��  ������ʱ�����ȼ۱�			    */
/* ˵  ��  					    */
/****************************************************/
void FindTempEqua(ArgRecord arg,TempEqua Entry)
{	 
    TempEqua tItem = tempEquaT;
    while (tItem!=null)
    { /*ע����Ϊ����ʱ���������������ֱ��ʹ�����ñȽ�
	��һ��������У���Ϊͬһ���������ܻ����
	���������ͬ��ARG�ṹ�������м��������ʱ��
	�����ֶ�*/
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
/* ������  SearchValuNum			    */
/* ��  ��  ���ұ����				    */
/* ˵  ��  �������ڱ�����з�����ڵ�ַ�����򷵻ؿ� */
/****************************************************/
void SearchValuNum(ArgRecord arg,ValuNum Entry)
{   
    boolean equal = false;
    /*ָ������*/
    ValuNum vItem = valuNumT;
    while (vItem != null)
    {   /*�Ƚ��Ƿ�����ͬ�ı���*/  
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
/* ������  IsEqual				    */
/* ��  ��  �ж�����ARG�ṹ�Ƿ���ͬ		    */
/* ˵  ��  �����������û�б�ţ���ֻ�����˳�����   */
/*	   �͵�ַ��ARG�ṹ			    */
/****************************************************/
boolean IsEqual(ArgRecord arg1,ArgRecord arg2)
{
    boolean equal = false;
    /*ע��Ӧ�Ƚ�ARG�ṹ���ݣ����ܱȽ����ã���Ϊһ����ͬ�ı���
      ���ܻ���������ͬ��ARG�ṹ���ɲ�ͬ�����ã�����
      ���м��������ʱ�Ĵ�����Ծ�����*/
    if (arg1.form == arg2.form)
    {
        if (arg1.form.equals("ValueForm"))
	{/*�����ֵࣺ�����ȼ�*/
	    if (arg1.midAttr.value == arg2.midAttr.value)
	        equal = true;
	}
	 /*��ַ�ࣺ������ƫ�ƣ����ʷ�ʽ�����ʱ�ȼ�*/
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
/* ������  AppendValuNum			    */
/* ��  ��  ��ǰ������ֵд��ֵ�������		    */
/* ˵  ��  ����һ���½ڵ㣬���ݱ����Ƿ�Ϊ�����ʱ�� */
/*	   ��д��ͬ�����ݣ����������		    */
/****************************************************/
void AppendValuNum(ArgRecord arg,int Vcode)
{   
    /*���һ���ڵ�����*/
    ValuNum last;
    /*����һ���µ�ֵ�����Ľڵ�,����д����*/
    ValuNum newItem = new ValuNum();
    newItem.arg = arg;
    /*���Ǽ����ʱ����*/
    if ((arg.form.equals("AddrForm"))&&(arg.midAttr.addr.dataLevel == -1)&&(arg.midAttr.addr.access.equals("indir")))
    { 
	newItem.access = "indir";
        newItem.codeInfo.twoCode = new TwoCode();
        newItem.codeInfo.twoCode.valuecode = Vcode;
	newItem.codeInfo.twoCode.addrcode = Vcode;
    }
    else 
    {
        /*�������Ϊ���Ǽ����ʱ����*/		
	newItem.access = "dir";
	newItem.codeInfo.valueCode = Vcode;
    }	
    /*�ڵ�����ֵ�������*/
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
/* ������  AppendTempEqua			    */
/* ��  ��  �������ȼ۵���ʱ����д����ʱ�����ȼ۱��� */
/* ˵  ��  ��ʾ��һ����Ա��ڶ����滻		    */
/****************************************************/
void AppendTempEqua(ArgRecord arg1,ArgRecord arg2)
{
    /*���һ���ڵ�ָ��*/
    TempEqua last;
    /*����һ���µ���ʱ�����ȼ۱�Ľڵ�,����д����*/
    TempEqua newItem = new TempEqua();
    newItem.arg1 = arg1;
    newItem.arg2 = arg2;

    /*�ڵ�������ʱ�����ȼ۱���*/
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
/* ������  AppendUsExpr				    */
/* ��  ��  ���м�������Ӧ��ӳ����д���ñ��ʽ���� */
/* ˵  ��					    */
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
/* ������  FindECC				    */
/* ��  ��  �жϿ��ñ��ʽ�����Ƿ��п��õı��ʽ���� */
/* ˵  ��  ���У����������滻���м��������	    */
/*	   ���򣬷���Ϊ��			    */
/****************************************************/
void FindECC(String codekind,int op1Code,int op2Code,CodeFile substiCode)
{   
    UsableExpr currentItem = usableExprT;

    while ((currentItem!=null)&&(substiCode.codeR.arg3==null))
    {   /*��������ͬ*/
	if (currentItem.code.codeR.codekind == codekind)
	/*��Ӧ�������붼��ͬ,���滻*/
	    if ((currentItem.mirrorC.op1==op1Code)&&(currentItem.mirrorC.op2==op2Code))
            {
	        substiCode.codeR = currentItem.code.codeR;
                substiCode.former = currentItem.code.former;
                substiCode.next = currentItem.code.next;
            }
	    else
            {
	        /*�ɽ�����������������뽻����ͬ��Ҳ���滻*/
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
/* ������  GenMirror				    */
/* ��  ��  ����ӳ����				    */
/* ˵  ��					    */
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
/* ������  SubstiVcode				    */
/* ��  ��  ����ǰ����������ֵ����д������	    */
/* ˵  ��  �������״γ��֣������һ����򣬽����� */
/*	   �˱�����ֵ�����滻Ϊ�µ�ֵ����	    */
/****************************************************/	
void SubstiVcode(ArgRecord arg,int Vcode)
{
    ValuNum Entry = new ValuNum();
    SearchValuNum(arg,Entry);

    /*���������״γ��֣�����������*/
    if (Entry.access==null)
	AppendValuNum(arg,Vcode);
    else 
    { 
	/*�����ʱ����*/
	if (Entry.access.equals("indir"))
	    Entry.codeInfo.twoCode.valuecode = Vcode;			
	/*�Ǽ����ʱ����*/
	else    
            Entry.codeInfo.valueCode = Vcode;	
    }		
}
/****************************************************/
/* ������  DelUsExpr				    */
/* ��  ��  �����ñ��ʽ��������õ�arg��ֵ�������  */
/*	   ɾ��					    */
/* ˵  ��					    */
/****************************************************/	
void DelUsExpr(ArgRecord arg)
{
    boolean same = false;
    UsableExpr Item = usableExprT;
    UsableExpr former = Item;  
    while (Item!=null)
    {   /*��ΪAADD�õ��ǵ�ַ�룬���Բ�����*/
	if (!(Item.code.codeR.codekind.equals("AADD")))
	{
            if ((Item.code.codeR.arg1 == arg)||(Item.code.codeR.arg2 == arg)||(Item.code.codeR.arg3 == arg))
	        same = true;
	    if (same) 
	    {   /*ɾ��������ñ��ʽ��*/ 
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
                /*������һ��ѭ����ʼ��*/
		continue;
	    }
	}
        /*ָ����ƣ��Ƚ���һ���ڵ�*/
	former = Item;
	Item = Item.next;
    }
}

/****************************************************/
/* ������  LoopOpti				    */
/* ��  ��  ѭ������ʽ�Ż�������		            */
/* ˵  ��					    */
/****************************************************/
CodeFile LoopOpti()
{   
    /*�ӵ�һ�����뿪ʼ�Ż�����*/
    CodeFile currentCode = firstCode;

    /*ѭ������ÿ�����룬ֱ���м�������*/
    while (currentCode!=null)
    {   
	if ((currentCode.codeR.codekind.equals("AADD"))||(currentCode.codeR.codekind.equals("ADD"))||(currentCode.codeR.codekind.equals("SUB"))||(currentCode.codeR.codekind.equals("MULT"))||(currentCode.codeR.codekind.equals("DIV"))||(currentCode.codeR.codekind.equals("LTC"))||(currentCode.codeR.codekind.equals("EQC")))	
	    /*������ֵ�ı��������������ֵ����*/
	    AddTable(currentCode.codeR.arg3);
        else if (currentCode.codeR.codekind.equals("ASSIG"))
	    /*������ֵ�ı������������ֵ����*/
	    AddTable(currentCode.codeR.arg2);
        else if (currentCode.codeR.codekind.equals("WHILESTART"))
	    /*ѭ�����*/
	    whileEntry(currentCode);
        else if (currentCode.codeR.codekind.equals("ENDWHILE"))
	    /*ѭ������*/
	    whileEnd(currentCode);
        else if (currentCode.codeR.codekind.equals("CALL"))
	    /*���̵������*/
	    call(currentCode);

        /*��ָ��ָ����һ������*/
	currentCode = currentCode.next;	 
    }
    return firstCode;
}
/****************************************************/
/* ������  whileEntry				    */
/* ��  ��  ѭ����ڲ��ֵĴ�����		    */
/* ˵  ��					    */
/****************************************************/
void whileEntry(CodeFile code)
{
    LoopInfo infoItem = new LoopInfo();

    /*�����־��ʼ��Ϊ���������־1*/
    infoItem.state = 1;
    /*��ѭ���ڱ�����ֵ������*/
    infoItem.varDef = TotalNum;
    /*ѭ�����ָ��*/
    infoItem.whileEntry = code;
    /*ѭ�����ڴ˴�����ȷ��*/
    infoItem.whileEnd = null;
    /*ѭ����Ϣ��ѹջ*/
    PushLoop(infoItem);
}
/****************************************************/
/* ������  call					    */
/* ��  ��  �������̵��������ر���		    */
/* ˵  ��  ���а����˵�������ѭ��������������ʽ   */
/*	   ����					    */
/****************************************************/
void call(CodeFile code)
{ 
    /*���д��ŵ�ѭ����Ϊ��������״̬������Щѭ����Ϣ�е�
      Stateȡ0*/
    LoopStack Item = loopTop;
  
    while (Item!=null)
    { 
        Item.loopInfo.state = 0;
        Item = Item.under;
    }
}
/****************************************************/
/* ������  whileEnd				    */
/* ��  ��  ѭ�����ڲ��ֵĴ�����		    */
/* ˵  ��					    */
/****************************************************/
void whileEnd(CodeFile code)
{
    /*ѭ����Ϣջ��ջ��*/
    LoopStack Item = loopTop;

    /*��������*/
    if (Item.loopInfo.state==1)
    {   
	/*��дѭ������λ�õ�����*/
	loopTop.loopInfo.whileEnd = code;
	/*�ҵ�ѭ�����*/
        CodeFile entry  = loopTop.loopInfo.whileEntry;
        /*ѭ�����ᴦ����*/
        LoopOutside(entry);
    }

    /*��ѭ����Ϣջ���˲�ѭ���������*/
    PopLoop();
}
/****************************************************/
/* ������  LoopOutside				    */
/* ��  ��  ѭ�����ᴦ����			    */
/* ˵  ��					    */
/****************************************************/
void LoopOutside(CodeFile entry)
{
    /*�����λ�ã�Ϊѭ�����λ��*/
    CodeFile place = entry;
    /*��ǰ�������,ע������ѭ����ʼ������*/
    CodeFile code =  entry.next;
   
    /*ȡѭ����Ϣջ��ָ��*/
    LoopStack Item = loopTop;
    /*ȡ�ñ���ѭ���ĳ���λ��*/
    CodeFile end = Item.loopInfo.whileEnd;
    /*ȡ�ñ���ѭ���ı�����Ϣ��*/
    int head = Item.loopInfo.varDef;
    int present1, present2;

    /*���������ڲ�ѭ��*/
    int Level = 0;

    /*ѭ�����ÿ�������Ƿ�������ᣬֱ���˲�ѭ������*/
    while (code!=end)
    {   
	if (code.codeR.codekind.equals("WHILESTART"))
	    Level++;	
	else if (code.codeR.codekind.equals("ENDWHILE"))  
	    Level--;
	else if ((code.codeR.codekind.equals("ADD"))||(code.codeR.codekind.equals("SUB"))||(code.codeR.codekind.equals("MULT"))||(code.codeR.codekind.equals("AADD")))	
        {
	    /*�����ڲ�ѭ��*/
	    if (Level==0)
	    {
		present1 = SearchTable(code.codeR.arg1,head);
		present2 = SearchTable(code.codeR.arg2,head);
		/*�������������ڱ�����ֵ����У���������*/
		if ((present1<0)&&(present2<0))
		{  
                    /*�������Ҳ�ǲ������������ڱ��У��ӱ���ɾ��*/
		    DelItem(code.codeR.arg3,head);
		    /*����*/
		    /*�ڵ�ǰλ�ã�ɾ���˴���*/
		    CodeFile formerCode = code.former;
		    CodeFile nextCode = code.next;
		    formerCode.next = nextCode;
		    nextCode.former = formerCode;

		    /*��������뵽Ӧ�����λ��*/
		    CodeFile fplace = place.former;
		    fplace.next  = code;
		    code.former = fplace;
  		    code.next = place;
		    place.former = code;

		    /*�ص���ǰλ�ô���׼��������һ�����*/
		    code = formerCode;					
		}
		else
		    /*���򣬽�������ֵ���뵱ǰ������ֵ����*/ 
		    AddTable(code.codeR.arg3);
	    }
	}
        /*�����һ�����*/
	code = code.next;
    }
}
/****************************************************/
/* ������  SearchTable				    */
/* ��  ��  ѭ��������ֵ����Һ���		    */
/* ˵  ��  ����headָ���˱���ѭ���ı�����ֵ�ڱ��е� */
/*         ��ʼλ�ã�arg��ʾҪ���ҵı���,���ر���   */
/*	   �ڱ��е�λ�ã��������ڷ���ֵΪ��1	    */
/****************************************************/
int SearchTable(ArgRecord arg,int head)
{
    /*��ʼ��Ϊ���������ٱ���*/
    int present = -1 ;

    if (arg.form.equals("AddrForm"))
    {   
	int level = arg.midAttr.addr.dataLevel;
	int off = arg.midAttr.addr.dataOff;
	/*ע����ʱ������Դ����������ͨ���Ƚϲ�����ƫ�ƿ��Ƿ����
	  �ڱ���*/
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
/* ������  DelItem				    */
/* ��  ��  ɾ��������ֵ���д���			    */
/* ˵  ��					    */
/****************************************************/
void DelItem(ArgRecord arg,int head)
{
    /*���ú������ұ�����ֵ��*/
    int present = SearchTable(arg,head);
    /*���ڱ��У���ɾ��*/
    if (present!=-1)
    {   
        for (int i=present;i<TotalNum;i++)
            varTable[i] =varTable[i+1];
        TotalNum--;
    }
}
/****************************************************/
/* ������  AddTable			            */
/* ��  ��  ������ֵ�ı������������ֵ��		    */
/* ˵  ��					    */
/****************************************************/
void AddTable(ArgRecord arg)
{  
    /*������ѭ���У����ͷ�����������ظ�������ͬ�ı���*/
    int head = 0;
    /*����ѭ���У���ֻҪ�ڵ�ǰѭ����û���ظ����弴��*/
    if (loopTop!=null)
	head = loopTop.loopInfo.varDef;

    int present = SearchTable(arg,head);
    /*����û�У������*/
    if (present==-1)
    {
       varTable[TotalNum] = arg;
       TotalNum = TotalNum+1;
    }
}
/****************************************************/
/* ������  PushLoop			            */
/* ��  ��  ѭ����Ϣջ��ѹջʵ�ֹ���		    */
/* ˵  ��					    */
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
/* ������  PopLoop			            */
/* ��  ��  ѭ����Ϣջ�ĵ�ջʵ�ֹ���		    */
/* ˵  ��					    */
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
/* ��  �� AnalYuyi	                                            */
/* ��  �� �ܳ���Ĵ���					            */
/* ˵  �� ����һ���࣬�����ܳ���                                    */
/********************************************************************/
class AnalYuyi
{
/* SCOPESIZEΪ���ű�scopeջ�Ĵ�С*/
int SCOPESIZE = 1000;

/*scopeջ*/
SymbTable scope[]=new SymbTable[SCOPESIZE];

/*��¼��ǰ����*/
int  Level=-1;
/*��¼��ǰƫ�ƣ�*/
int  Off;
/*��¼�������displayOff*/
int mainOff;
/*��¼��ǰ���displayOff*/
int savedOff;

/*ע�����Ա��ƫ������0��ʼ��*/
int fieldOff = 0;

/*��¼������display���ƫ����*/
int StoreNoff;

/*����Ŀ�����������Ҫ��initOffӦΪAR�׵�ַsp���βα�������ƫ��7*/	
int initOff=7;

/*�ֱ�ָ�����ͣ��ַ��ͣ�bool���͵��ڲ���ʾ*/
TypeIR  intptr = new TypeIR();
TypeIR  charptr = new TypeIR();
TypeIR  boolptr = new TypeIR();

/*����׷�ٱ�־*/
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
/* ������  Analyze				    */
/* ��  ��  �������������        		    */
/* ˵  ��  ���﷨���ĸ��ڵ㿪ʼ�������������       */	
/****************************************************/
void Analyze(TreeNode t)	
{ 
    TreeNode p = null;
    TreeNode pp = t;

    /*����һ���µķ��ű���ʼ�������*/
    CreatSymbTable();

    /*���������ڲ���ʾ��ʼ������*/
    initiate();

    /*�﷨���������ڵ�*/
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
        p = p.sibling ;/*ѭ������*/
     }
	
    /*������*/
    t = t.child[2];
    if(t.nodekind.equals("StmLK"))
	BodyA(t);
	
    /*�������ű�*/
    if (Level!=-1)
        DestroySymbTable();
	
    /*����������*/
    if(Error)
	AnalyzeError(null," Analyze Error ",null);
} 

/****************************************************/
/* ������  TypeDecPart				    */
/* ��  ��  ����һ����������     		    */
/* ˵  ��  �����﷨���е����������ڵ㣬ȡ��Ӧ���ݣ� */
/*	   �����ͱ�ʶ��������ű�.	            */
/****************************************************/            
void TypeDecPart(TreeNode t)
{ 
    boolean present=false;
    AttributeIR Attrib=new AttributeIR();  /*�洢��ǰ��ʶ��������*/
    SymbTable entry = new SymbTable();

    Attrib.kind="typekind";  
    while (t!=null)   
    {
	/*���ü�¼���Ժ����������Ƿ��ظ����������ڵ�ַ*/
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
/* ������  TYPEA 				    */
/* ��  ��  �������͵��ڲ���ʾ     		    */
/* ˵  ��  ���þ������ʹ�����������ڲ���ʾ�Ĺ���   */
/*	   ����ָ�������ڲ���ʾ��ָ��.	            */
/****************************************************/   
TypeIR TYPEA(TreeNode t,String kind)
{ 
    TypeIR typeptr=null;
	
    /*���ݲ�ͬ������Ϣ��������Ӧ�����ʹ�����*/
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
/* ������  NameTYPEA				    */
/* ��  ��  ��������Ϊ���ͱ�ʶ��ʱ������	            */
/* ˵  ��  �������µ����ͣ����ش����ͱ�ʶ�������ͣ� */
/*	   ������������                           */
/****************************************************/  
TypeIR NameTYPEA(TreeNode t)
{  
    SymbTable Entry=new SymbTable();                     
    TypeIR temp=null;
    boolean present;

    present= FindEntry(t.attr.type_name,Entry);
    /*������ͱ�ʶ��δ������*/
    if (!present)
	AnalyzeError(t," id use before declaration ",t.attr.type_name);
    /*�������ͱ�ʶ����*/  
    else if (!(Entry.attrIR.kind.equals("typekind")))
	AnalyzeError(t," id is not type id ",t.attr.type_name);
    /*���ر�ʶ�������͵��ڲ���ʾ*/
    else
    {  
        temp= Entry.attrIR.idtype;
        return temp;
    }
    return temp;
}	 
/****************************************************/
/* ������  ArrayTypeA				    */
/* ��  ��  �����������͵��ڲ���ʾ   		    */
/* ˵  ��  �����±����ͣ���Ա���ͣ����������С��   */
/*	   ������±곬�����		            */
/****************************************************/   
TypeIR ArrayTYPEA(TreeNode t)
{ 
    TypeIR tempforchild;

    /*����һ���µ��������͵��ڲ���ʾ*/
    TypeIR typeptr=new TypeIR();
    typeptr.array=new Array();
    typeptr.kind="arrayTy";
    /*�±���������������*/
    typeptr.array.indexTy=intptr;                         
    /*��Ա����*/
    tempforchild=TYPEA(t,t.attr.arrayAttr.childtype);
    typeptr.array.elementTy=tempforchild;

    /*��������±�������*/
    int up=t.attr.arrayAttr.up;
    int low=t.attr.arrayAttr.low;
    if (up < low)
	AnalyzeError(t," array up smaller than under ",null);
    else  /*���½�������������ڲ���ʾ��*/
    {       
        typeptr.array.low = low;
        typeptr.array.up = up;
    }
    /*��������Ĵ�С*/
    typeptr.size= (up-low+1)*(tempforchild.size);
    /*����������ڲ���ʾ*/
    return typeptr;
}
/****************************************************/
/* ������  RecordTYPEA				    */
/* ��  ��  �����¼���͵��ڲ���ʾ   		    */
/* ˵  ��  �������ָ��洢�ڼ�¼���ڲ���ʾ�У�   */
/*	   �������¼�Ĵ�С                         */  
/****************************************************/
TypeIR RecordTYPEA(TreeNode t)
{ 
    TypeIR Ptr=new TypeIR();  /*�½���¼���͵Ľڵ�*/
    Ptr.body=new FieldChain();
    Ptr.kind="recordTy";
	
    t = t.child[0];                /*���﷨���Ķ��ӽڵ��ȡ����Ϣ*/

    FieldChain Ptr2=null;
    FieldChain Ptr1=null;
    FieldChain body=null;

    while (t != null)				/*ѭ������*/
    {
	/*��дptr2ָ������ݽڵ�*
	 *�˴�ѭ���Ǵ���������int a,b; */
	for(int i=0 ; i < t.idnum ; i++)
	{     
	    /*�����µ������͵�Ԫ�ṹPtr2*/  
	    Ptr2 = new FieldChain();            
	    if(body == null)
            {
		body = Ptr2; 
                Ptr1 = Ptr2;
	    }
	    /*��дPtr2�ĸ�����Ա����*/
	    Ptr2.id=t.name[i];
	    Ptr2.unitType = TYPEA(t,t.kind);			 
	    
	    /*���Ptr1!=Ptr2����ô��ָ�����*/
	    if(Ptr2 != Ptr1)          
	    {
		/*����������ĵ�Ԫoff*/
		Ptr2.off = (Ptr1.off) + (Ptr1.unitType.size);
		Ptr1.next = Ptr2;
		Ptr1 = Ptr2;
	    }
	}
	/*������ͬ���͵ı�����ȡ�﷨�����ֵܽڵ�*/
	t = t.sibling;
    }	
    /*�����¼�����ڲ��ṹ*/
	
    /*ȡPtr2��offΪ���������¼��size*/
    Ptr.size = Ptr2.off + (Ptr2.unitType.size);
    /*�����������¼���͵�body����*/   
    Fcopy(Ptr.body,body);   

    return Ptr;
}
/****************************************************/
/* ������  VarDecPart				    */
/* ��  ��  �����������з�������    		    */
/* ˵  ��  �������еı�������			    */
/****************************************************/
void VarDecPart(TreeNode t) 
{  
    varDecList(t);
}   
/****************************************************/
/* ������  varDecList 				    */
/* ��  ��  ����һ�������������β�����		    */
/* ˵  ��  ����һ�������ڵ������������б�ʶ����	    */	
/*	   �������Ϣ������ű��У������βΣ���Ҫ   */
/*	   ����һ��������Ϣ��������ʶ���ڷ��ű�� */
/*	   λ�ô洢�ڱ��У����ز�����ı�ͷָ��     */					
/****************************************************/
void varDecList(TreeNode t)
{ 
    boolean present = false;
    SymbTable  Entry=new SymbTable();
    /*��¼����������*/
    AttributeIR Attrib=new AttributeIR();

    while(t!=null)	/*ѭ������*/
    {
	Attrib.kind="varkind";  
	for(int i=0;i<(t.idnum);i++)
	{
	    Attrib.idtype=TYPEA(t,t.kind);
			
	    /*�ж�ʶֵ�λ��Ǳ��acess(dir,indir)*/	
	    if((t.attr.procAttr!=null)&&(t.attr.procAttr.paramt.equals("varparamType")))
	    {
                Attrib.var = new Var();
		Attrib.var.access = "indir";
		Attrib.var.level = Level;
		/*�����βε�ƫ��*/
				
		Attrib.var.off = Off;
		Off = Off+1;
	    }/*����Ǳ�Σ���ƫ�Ƽ�1*/
	    else
	    {
                Attrib.var = new Var();
		Attrib.var.access = "dir";
		Attrib.var.level = Level;
		/*����ֵ�ε�ƫ��*/
		if(Attrib.idtype.size!=0)				
		{
		    Attrib.var.off = Off;
		    Off = Off + (Attrib.idtype.size);
		}
	    }/*���������Ϊֵ�Σ�ƫ�Ƽӱ������͵�size*/
			
	    /*�ǼǸñ��������Լ�����,�������������ڲ�ָ��*/
	    present = Enter(t.name[i],Attrib,Entry);	
	    if(present)
	        AnalyzeError(t," id repeat  declaration ",t.name[0]);
	    else
	        t.table[i] = Entry;
	}
	if(t!=null)
	    t = t.sibling;
    }
	
    /*��������������¼��ʱƫ�ƣ�����Ŀ���������ʱ��displayOff*/
    if(Level==0)
    {
	mainOff = Off;
	/*�洢������AR��display���ƫ�Ƶ�ȫ�ֱ���*/
	StoreNoff = Off;
    }
    /*����������������¼��ʱƫ�ƣ�����������д������Ϣ���noff��Ϣ*/ 
    else 
	savedOff = Off;
} 
/****************************************************/
/* ������  procDecPart				    */
/* ��  ��  һ�������������������  		    */
/* ˵  ��  �������ͷ��������������		    */	
/****************************************************/
void procDecPart(TreeNode t)
{ 
    TreeNode p =t;
    SymbTable entry = HeadProcess(t);   /*�������ͷ*/
		
    t = t.child[1];
    /*��������ڲ������������֣�������������*/	
    while (t!=null) 
    {
	if ( t.nodekind.equals("TypeK") ) 
	    TypeDecPart(t.child[0]); 
        else if ( t.nodekind.equals("VarK") )  
            VarDecPart(t.child[0]);  

	/*������������к���������������ѭ��������дnoff��moff����Ϣ��*
	*�ٴ�����������ѭ�����������޷�����noff��moff��ֵ��      */
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
    /*���̻��¼�ĳ��ȵ���nOff����display��ĳ���*
    *diplay��ĳ��ȵ��ڹ������ڲ�����һ           */

    /*����������������*/
    while(t!=null)
    {
	procDecPart(t);
	t = t.sibling;
    }
    t = p;
    BodyA(t.child[2]);/*����Block*/

    /*�������ֽ�����ɾ�������β�ʱ���½����ķ��ű�*/
    if ( Level!=-1)
	DestroySymbTable();/*������ǰscope*/
}
/****************************************************/
/* ������  HeadProcess				    */
/* ��  ��  �βδ�����         		    */
/* ˵  ��  ѭ����������ڵ㣬��������ÿ���ڵ�õ�   */
/*	   �Ĳ�����������������������β��������� */
/*         ������ָ��				    */	
/****************************************************/
SymbTable HeadProcess(TreeNode t)
{ 
    AttributeIR attrIr = new AttributeIR();
    boolean present = false;
    SymbTable entry = new SymbTable();
		
    /*������*/
    attrIr.kind = "prockind";
    attrIr.idtype = null; 
    attrIr.proc = new Proc();
    attrIr.proc.param = new ParamTable();
    attrIr.proc.level = Level+1;	
	
    if(t!=null)
    {
	/*�ǼǺ����ķ��ű���*/		
	present = Enter(t.name[0],attrIr,entry);
	t.table[0] = entry;
	/*�����β�������*/
    }
    entry.attrIR.proc.param = ParaDecList(t);

    return entry;
}   
/****************************************************/
/* ������  ParaDecList				    */
/* ��  ��  ����һ���βνڵ�        		    */
/* ˵  ��  ���ݲ������βλ��Ǳ�Σ��ֱ���ñ������� */
/*	   �ڵ�Ĵ���������һ��ʵ����Add����ʾ����*/
/*	   ���Ǻ������βΡ�			    */	
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
	    p = t.child[0];   	/*���������ڵ�ĵ�һ�����ӽڵ�*/
	
	CreatSymbTable();		/*�����µľֲ�����*/
	Off = 7;                /*�ӳ����еı�����ʼƫ����Ϊ8*/

	VarDecPart(p);			/*������������*/

	SymbTable Ptr0 = scope[Level];      		 
                                    
	while(Ptr0 != null)         /*ֻҪ��Ϊ�գ��ͷ������ֵܽڵ�*/
	{
	    /*�����βη��ű���ʹ�����������ű��param��*/
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
    return head;   /*�����βη��ű��ͷָ��*/
}
/****************************************************/
/* ������  BodyA				    */
/* ��  ��  ������д�����        		    */
/* ˵  ��  ���ڴ����������߳����壬		    */
/*	  ѭ������������			    */	
/****************************************************/
void BodyA(TreeNode t)
{  
    /*��ָ��ָ���һ�����*/
    if (t.nodekind.equals("StmLK"))
	t=t.child[0];

    /*�����������*/
    while (t!=null)
    { 
        /*������䴦����*/
	StatementA(t);
        t= t.sibling;
    }
}
/****************************************************/
/* ������  StatementA				    */
/* ��  ��  ��䴦����	        		    */
/* ˵  ��  �������ľ������ͣ��ֱ������Ӧ��       */
/*	   ��䴦����				    */
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
/* ������  AssignSA				    */
/* ��  ��  ��ֵ��䴦����	       		    */
/* ˵  ��  ����󲿱�ʶ�������ñ��ʽ��������	    */	
/*	   ������ʶ��δ��������������ʶ����   */
/*	   ��ֵ�����ݴ�				    */
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
	/*�ڷ��ű��в��Ҵ˱�ʶ��*/
	present = FindEntry(child1.name[0],entry);
		
	if(present)
	{   /*id���Ǳ���*/
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
	else /*��ʶ��������*/
	    AnalyzeError(t,"is not declarations!",child1.name[0]);
	}
	else/*Var0[E]������*/
	{	
            if(child1.attr.expAttr.varkind.equals("ArrayMembV"))
		Eptr = arrayVar(child1);	
	    else /*Var0.id������*/
		if(child1.attr.expAttr.varkind.equals("FieldMembV"))
		    Eptr = recordVar(child1);
	}
	if(Eptr != null)
	{	
	    if((t.nodekind.equals("StmtK"))&&(t.kind.equals("AssignK")))
	    {
		/*����ǲ��Ǹ�ֵ������ ���͵ȼ�*/
		ptr = Expr(child2,null);
		if (!Compat(ptr,Eptr)) 
		    AnalyzeError(t,"ass_expression error!",child2.name[0]);
	    }
	    /*��ֵ����в��ܳ��ֺ�������*/
	}
}
/***********************************************************/
/* ������ Compat                                           */
/* ��  �� �ж������Ƿ�����                                 */
/* ˵  �� ����TINY������ֻ���������͡��ַ����͡��������ͺ� */
/*        ��¼���ͣ����������ݵ������͵ȼۣ�ֻ���ж�ÿ���� */
/*        �����͵��ڲ���ʾ������ָ��ֵ�Ƿ���ͬ���ɡ�       */
/***********************************************************/
boolean Compat(TypeIR tp1,TypeIR tp2)
{
    boolean  present; 
    if (tp1!=tp2)
	present = false;  /*���Ͳ���*/
    else
	present = true;   /*���͵ȼ�*/
    return present;
}

/************************************************************/
/* ������  Expr                                             */
/* ��  ��  �ú���������ʽ�ķ���                           */
/* ˵  ��  ���ʽ����������ص��Ǽ��������������������ԣ� */
/*         ����ʽ�����͡����в���Ekind������ʾʵ���Ǳ��  */
/*         ����ֵ�Ρ�    	                            */
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
	        Ekind = "dir";   /*ֱ�ӱ���*/ 
        }
        else if(t.kind.equals("VariK"))
        {
	    /*Var = id������*/
	    if(t.child[0]==null)
	    {	
		/*�ڷ��ű��в��Ҵ˱�ʶ��*/
		present = FindEntry(t.name[0],entry);				
		t.table[0] = entry;

		if(present)
		{   /*id���Ǳ���*/
		    if (!(entry.attrIR.kind.equals("varkind")))
		    {
			AnalyzeError(t," syntax bug: no this kind of exp ",t.name[0]);				                              Eptr = null;
		    }
		    else
		    {
			Eptr = entry.attrIR.idtype;	
			if (Ekind!=null)
			    Ekind = "indir";  /*��ӱ���*/						
		    }
		} 
		else /*��ʶ��������*/
		    AnalyzeError(t,"is not declarations!",t.name[0]);				
	    }
	    else/*Var = Var0[E]������*/
	    {	
                if(t.attr.expAttr.varkind.equals("ArrayMembV"))
		    Eptr = arrayVar(t);	
		/*Var = Var0.id������*/
		else if(t.attr.expAttr.varkind.equals("FieldMembV"))
		    Eptr = recordVar(t);
	    }
	}
        else if(t.kind.equals("OpK"))
        {
	    /*�ݹ���ö��ӽڵ�*/
	    Eptr0 = Expr(t.child[0],null);
	    if(Eptr0==null)
	        return null;
	    Eptr1 = Expr(t.child[1],null);
	    if(Eptr1==null)
		return null;
							
	    /*�����б�*/
	    present = Compat(Eptr0,Eptr1);
	    if (present)
	    {
		if((t.attr.expAttr.op.equals("LT"))||(t.attr.expAttr.op.equals("EQ")))
		    Eptr = boolptr;
                else if((t.attr.expAttr.op.equals("PLUS"))||(t.attr.expAttr.op.equals("MINUS"))||(t.attr.expAttr.op.equals("TIMES"))||(t.attr.expAttr.op.equals("OVER")))  
		    Eptr = intptr;
                                /*�������ʽ*/
		if(Ekind != null)
	            Ekind = "dir"; /*ֱ�ӱ���*/
	    }
	    else 
		AnalyzeError(t,"operator is not compat!",null);
	}
    }
    return Eptr;
}			

/************************************************************/
/* ������  arrayVar                                         */
/* ��  ��  �ú�����������������±����                     */
/* ˵  ��  ���var := var0[E]��var0�ǲ����������ͱ�����E�ǲ�*/
/*         �Ǻ�������±��������ƥ�䡣                     */
/************************************************************/
TypeIR arrayVar(TreeNode t)
{
    boolean present = false;
    SymbTable entry = new SymbTable();

    TypeIR Eptr0=null;
    TypeIR Eptr1=null;
    TypeIR Eptr = null;
	
	
    /*�ڷ��ű��в��Ҵ˱�ʶ��*/

    present = FindEntry(t.name[0],entry);				
    t.table[0] = entry;	
    /*�ҵ�*/
    if(present)
    {
	/*Var0���Ǳ���*/
	if (!(entry.attrIR.kind.equals("varkind")))
	{
	    AnalyzeError(t,"is not variable error!",t.name[0]);			
	    Eptr = null;
	}
	/*Var0�����������ͱ���*/
	else if(entry.attrIR.idtype!=null)
        {
	    if(!(entry.attrIR.idtype.kind.equals("arrayTy")))
	    {
		AnalyzeError(t,"is not array variable error !",t.name[0]);
		Eptr = null;
	    }
	    else
	    {	
		/*���E�������Ƿ����±��������*/
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
    else/*��ʶ��������*/
	AnalyzeError(t,"is not declarations!",t.name[0]);
    return Eptr;
}

/************************************************************/
/* ������  recordVar                                        */
/* ��  ��  �ú��������¼��������ķ���                     */
/* ˵  ��  ���var:=var0.id�е�var0�ǲ��Ǽ�¼���ͱ�����id�� */
/*         ���Ǹü�¼�����е����Ա��                       */
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
	
	
	/*�ڷ��ű��в��Ҵ˱�ʶ��*/
	present = FindEntry(t.name[0],entry);				
	t.table[0] = entry;	
	/*�ҵ�*/
	if(present)
	{
	    /*Var0���Ǳ���*/
	    if (!(entry.attrIR.kind.equals("varkind")))
	    {
		AnalyzeError(t,"is not variable error!",t.name[0]);				
		Eptr = null;
	    }
	    /*Var0���Ǽ�¼���ͱ���*/
	    else if(!(entry.attrIR.idtype.kind.equals("recordTy")))
	    {
		AnalyzeError(t,"is not record variable error!",t.name[0]);
		Eptr = null;
	    }
	    else/*���id�Ƿ��ǺϷ�����*/
	    {
		Eptr0 = entry.attrIR.idtype;
		currentP = Eptr0.body;
		while((currentP!=null)&&(!result))
		{  
        	    result = t.child[0].name[0].equals(currentP.id);
		    /*������*/
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
	            /*���id���������*/
		    else if(t.child[0].child[0]!=null)
			Eptr = arrayVar(t.child[0]);
	    }
	}
	else/*��ʶ��������*/
	    AnalyzeError(t,"is not declarations!",t.name[0]);
	return Eptr;
}
		
/****************************************************/
/* ������  CallSA				    */
/* ��  ��  ����������䴦����    		    */
/* ˵  ��  ���Ǻ�����ʶ�������ü����ʵ���Ƿ�   */	
/*		   ���ݺ���			    */
/****************************************************/
void CallSA(TreeNode t)
{ 
	String Ekind=" ";
	boolean present = false;
	SymbTable entry=new SymbTable();
	TreeNode p = null;

	/*��id����������ű�*/
	present = FindEntry(t.child[0].name[0],entry);		
        t.child[0].table[0] = entry;

	/*δ�鵽��ʾ����������*/
	if (!present)                     
	    AnalyzeError(t,"function is not declarationed!",t.child[0].name[0]);  
        else 
	    /*id���Ǻ�����*/
	    if (!(entry.attrIR.kind.equals("prockind")))     
		AnalyzeError(t,"is not function name!",t.child[0].name[0]);
	    else/*��ʵ��ƥ��*/
	    {
		p = t.child[1];
		/*paramPָ���βη��ű�ı�ͷ*/
		ParamTable paramP = entry.attrIR.proc.param;	
		while((p!=null)&&(paramP!=null))
		{
		    SymbTable paraEntry = paramP.entry;
		    TypeIR Etp = Expr(p,Ekind);/*ʵ��*/
		    /*�������ƥ��*/
		    if ((paraEntry.attrIR.var.access.equals("indir"))&&(Ekind.equals("dir")))
			AnalyzeError(t,"param kind is not match!",null);  
			/*�������Ͳ�ƥ��*/
                    else if((paraEntry.attrIR.idtype)!=Etp)
			AnalyzeError(t,"param type is not match!",null);
		    p = p.sibling;
		    paramP = paramP.next;
		}
		/*����������ƥ��*/
		if ((p!=null)||(paramP!=null))
		    AnalyzeError(t,"param num is not match!",null); 
	    }
}
/****************************************************/
/* ������  ReadSA				    */
/* ��  ��  ����䴦����	    		    */
/* ˵  ��  ����ʶ��δ�������Ǳ�����ʶ����	    */
/****************************************************/
void ReadSA(TreeNode t)
{ 
    SymbTable Entry=new SymbTable();
    boolean present=false;
    /*���ұ�����ʶ��*/
    present = FindEntry(t.name[0],Entry);
    /*�����ڷ��ű��еĵ�ַд���﷨��*/
    t.table[0] = Entry;

    if (!present)   /*����ʶ��δ������*/
	AnalyzeError(t," id no declaration in read ",t.name[0]);
    else if (!(Entry.attrIR.kind.equals("varkind")))   /*���Ǳ�����ʶ����*/ 
        AnalyzeError(t," not var id in read statement ", null);
}
/****************************************************/
/* ������  WriteSA				    */
/* ��  ��  д��䴦����	    		    */
/* ˵  ��  ���ñ��ʽ������������������	    */
/****************************************************/
void WriteSA(TreeNode t)  
{ 
    TypeIR Etp = Expr(t.child[0],null);	
    if(Etp!=null)
	/*������ʽ����Ϊbool���ͣ�����*/
	if (Etp.kind.equals("boolTy"))
		AnalyzeError(t,"exprssion type error!",null);
}
/****************************************************/
/* ������  IfSA					    */
/* ��  ��  ������䴦����	    		    */
/* ˵  ��  ���ǲ������ʽ��������������к���   */
/*	   ����then���ֺ� else ����	            */	
/****************************************************/
void IfSA(TreeNode t)
{ 
    String Ekind=null;
    TypeIR Etp;
    Etp=Expr(t.child[0],Ekind);
    
    if (Etp!=null)   /*���ʽû�д���*/
        if (!(Etp.kind.equals("boolTy")))   /*���ǲ������ʽ��*/
	    AnalyzeError(t," not bool expression in if statement ",null);
	else
	{
	    TreeNode p = t.child[1];
	    /*����then������в���*/
	    while(p!=null)
	    {
		StatementA(p);
		p=p.sibling;
	    }
	    t = t.child[2];		/*����������*/
	    /*����else��䲿��*/
	    while(t!=null)
	    {
		StatementA(t);	
		t=t.sibling;
	    }
	}
}
/****************************************************/
/* ������  WhileSA				    */
/* ��  ��  ѭ����䴦����	    		    */
/* ˵  ��  ���ǲ������ʽ��������������к���   */
/*	   ����ѭ����			            */	
/****************************************************/
void  WhileSA(TreeNode t)
{ 
    TypeIR Etp;
    Etp=Expr(t.child[0],null);
   
    if (Etp!=null)  /*���ʽû�д�*/	  
        if (!(Etp.kind.equals("boolTy")))   /*���ǲ������ʽ��*/
	    AnalyzeError(t," not bool expression in if statement ",null);
    /*����ѭ����*/
    else
    {
	t = t.child[1];
	/*����ѭ������*/
	while(t!=null)
	{ 
	    StatementA(t);
	    t=t.sibling;
	}
    }
}
/****************************************************/
/* ������  ReturnSA				    */
/* ��  ��  ������䴦����	    		    */
/* ˵  ��  ���������������У����������		    */	
/****************************************************/
void  ReturnSA(TreeNode t)
{
    if (Level == 0)
	AnalyzeError(t," return statement cannot in main program ",null);
}

/****************************************************/
/*****************���ܺ���***************************/
/****************************************************/
/* ������  AnalyzeError				    */
/* ��  ��  �������������ʾ��Ϣ			    */
/* ˵  ��  Error����Ϊtrue,��ֹ����Ĵ���	    */
/****************************************************/
void AnalyzeError(TreeNode t,String message,String s)
{   
    if (t==null)
        yerror=yerror+"\n>>> ERROR:"+"Analyze error "+":"+message+s+"\n"; 
    else
        yerror=yerror+"\n>>> ERROR :"+"Analyze error at "+String.valueOf(t.lineno)+": "+message+s+"\n";  

    /* ���ô���׷�ٱ�־ErrorΪTRUE,��ֹ�����һ������ */
    Error = true;
}
/****************************************************/
/* ������  initiate				    */
/* ��  ��  �������ͣ��ַ��ͣ��������͵��ڲ���ʾ	    */
/* ˵  ��  �⼸�����͵��ڲ���ʾʽ�̶��ģ�ֻ�轨��   */
/*	   һ�Σ��Ժ�������Ӧ�����ü���	            */	
/****************************************************/
void initiate()
{   
    /*�������͵��ڲ���ʾ*/
    intptr.kind="intTy";
    intptr.size=1;
    /*�ַ����͵��ڲ���ʾ*/
    charptr.kind="charTy";
    charptr.size=1;
    /*�������͵��ڲ���ʾ*/
    boolptr.kind="boolTy";
    boolptr.size=1;
}

/********************************************************/
/*************���ű���غ���*****************************/
/********************************************************/
/* ������  CreatSymbTable			        */
/* ��  ��  ����һ�����ű�				*/
/* ˵  ��  ��û�����������µķ��ű�ֻ�ǲ�����һ	*/
/********************************************************/
void  CreatSymbTable()
{ 	
    Level = Level +1; 
    scope[Level] = null;	
    Off = initOff;  /* ����Ŀ�����������Ҫ��initOffӦΪAR�׵�ַsp
		       ���βα�������ƫ��7 */	
}
/********************************************************/
/* ������  DestroySymbTable				*/
/* ��  ��  ɾ��һ�����ű�				*/
/* ˵  ��  ���������ͷ�������ű�ռ䣬ֻ�Ǹı�scopeջ  */
/********************************************************/
void  DestroySymbTable()
{
    /*�ò�����1������ʾɾ����ǰ���ű�*/
    Level = Level - 1;
}
/**********************************************************/
/* ������  Enter					  */
/* ��  ��  ��һ����ʶ���������ԵǼǵ����ű�		  */
/* ˵  ��  ����ֵ������ʶ���Ƿ��ظ�����Entry���ش˱�ʶ��  */
/*         �ڷ��ű��е�λ�ã����ظ����򲻵Ǽǣ�EntryΪ��  */
/*	   �����Ǹ���ʶ����λ��				  */
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
	}   /*�ڸò���ű��ڼ���Ƿ����ظ��������*/
    
	if(!present)
	{
	    prentry.next = new SymbTable();
	    curentry = prentry.next;
	}
    }
		
    /*����ʶ���������ԵǼǵ�����*/
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
/* ������  FindEntry    				*/
/* ��  ��  ����һ����ʶ���Ƿ��ڷ��ű���			*/
/* ˵  ��  ����flag�����ǲ��ҵ�ǰ���ű��������з��ű� */
/*	   ����ֵ�����Ƿ��ҵ�������Entry���ش˱�ʶ����  */
/*	   ���ű��е�λ��				*/
/********************************************************/
boolean FindEntry(String id,SymbTable entry)
{ 
	boolean  present=false;  /*����ֵ*/
	boolean result = false;         /*��ʶ�����ֱȽϽ��*/
	int lev = Level;	 /*��ʱ��¼�����ı���*/

	SymbTable findentry = scope[lev];

	while((lev!=-1)&&(!present))
	{
	    while ((findentry!=null)&&(!present))
	    {
		result = id.equals(findentry.idName);
		if (result)
		    present = true;    
		/*�����ʶ��������ͬ���򷵻�TRUE*/
		else 
		    findentry = findentry.next;
		/*���û�ҵ�������������еĲ���*/
	    }
	    if(!present)
	    {
		lev = lev-1;
                if(lev != -1)
		    findentry = scope[lev];			
	    }
	}/*����ڱ�����û�в鵽����ת����һ���ֲ��������м�������*/
        if (!present)
	    entry = null;
	else 
	    copy(entry,findentry);

	return present;
}
/********************************************************/
/* ������  copy	  				        */
/* ��  ��  ���ƺ���    				        */
/* ˵  ��  ��b�е����ݸ��Ƹ�a			        */
/********************************************************/
void copy(SymbTable a,SymbTable b)
{
    a.idName=b.idName;
    a.attrIR=b.attrIR;
    a.next=b.next;
}
/********************************************************/
/* ������  Fcopy	  				*/
/* ��  ��  ���ƺ���    				        */
/* ˵  ��  ��b�е����ݸ��Ƹ�a			        */
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
/************************�� �� �� ��*********************************/
/********************************************************************/
/********************************************************************/
/* ��  �� Recursion	                                            */
/* ��  �� �ܳ���Ĵ���					            */
/* ˵  �� ����һ���࣬�����ܳ���                                    */
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
/* ������ Program					            */
/* ��  �� �ܳ���Ĵ�����					    */
/* ����ʽ < Program > ::= ProgramHead DeclarePart ProgramBody .     */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
/*        �﷨���ĸ��ڵ�ĵ�һ���ӽڵ�ָ�����ͷ����ProgramHead,    */
/*        DeclaraPartΪProgramHead���ֵܽڵ�,�����岿��ProgramBody  */
/*        ΪDeclarePart���ֵܽڵ�.                                  */
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

/**************************����ͷ����********************************/
/********************************************************************/
/********************************************************************/
/* ������ ProgramHead						    */
/* ��  �� ����ͷ�Ĵ�����					    */
/* ����ʽ < ProgramHead > ::= PROGRAM  ProgramName                  */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
    
/**************************��������**********************************/
/********************************************************************/	
/********************************************************************/
/* ������ DeclarePart						    */
/* ��  �� �������ֵĴ���					    */
/* ����ʽ < DeclarePart > ::= TypeDec  VarDec  ProcDec              */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
/********************************************************************/
TreeNode DeclarePart()
{
    /*����*/
    TreeNode typeP = newNode("TypeK");    	 
    TreeNode tp1 = TypeDec();
    if (tp1!=null)
        typeP.child[0] = tp1;
    else
	typeP=null;

    /*����*/
    TreeNode varP = newNode("VarK");
    TreeNode tp2 = VarDec();
    if (tp2 != null)
        varP.child[0] = tp2;
    else 
        varP=null;
		 
    /*����*/
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

/**************************������������******************************/
/********************************************************************/
/* ������ TypeDec					            */
/* ��  �� �����������ֵĴ���    				    */
/* ����ʽ < TypeDec > ::= �� | TypeDeclaration                      */
/* ˵  �� �����ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�      */
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
/* ������ TypeDeclaration					    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < TypeDeclaration > ::= TYPE  TypeDecList                 */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ TypeDecList		 				    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < TypeDecList > ::= TypeId = TypeName ; TypeDecMore       */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ TypeDecMore		 				    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < TypeDecMore > ::=    �� | TypeDecList                   */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ TypeId		 				    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < TypeId > ::= id                                         */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ TypeName		 				    */
/* ��  �� �����������ֵĴ���				            */
/* ����ʽ < TypeName > ::= BaseType | StructureType | id            */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ BaseType		 				    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < BaseType > ::=  INTEGER | CHAR                          */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ StructureType		 				    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < StructureType > ::=  ArrayType | RecType                */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ ArrayType                                                 */
/* ��  �� �����������ֵĴ�����			            */
/* ����ʽ < ArrayType > ::=  ARRAY [low..top] OF BaseType           */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ RecType		 				    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < RecType > ::=  RECORD FieldDecList END                  */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ FieldDecList		 				    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < FieldDecList > ::=   BaseType IdList ; FieldDecMore     */
/*                             | ArrayType IdList; FieldDecMore     */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ FieldDecMore		 				    */
/* ��  �� �����������ֵĴ�����			            */
/* ����ʽ < FieldDecMore > ::=  �� | FieldDecList                   */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ IdList		 				    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < IdList > ::=  id  IdMore                                */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ IdMore		 				    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < IdMore > ::=  �� |  , IdList                            */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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

/**************************������������******************************/
/********************************************************************/
/* ������ VarDec		 				    */
/* ��  �� �����������ֵĴ���				            */
/* ����ʽ < VarDec > ::=  �� |  VarDeclaration                      */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ VarDeclaration		 			    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < VarDeclaration > ::=  VAR  VarDecList                   */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ VarDecList		 				    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < VarDecList > ::=  TypeName VarIdList; VarDecMore        */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ VarDecMore		 				    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < VarDecMore > ::=  �� |  VarDecList                      */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ VarIdList		 				    */
/* ��  �� �����������ֵĴ�����			            */
/* ����ʽ < VarIdList > ::=  id  VarIdMore                          */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ VarIdMore		 				    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < VarIdMore > ::=  �� |  , VarIdList                      */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/****************************������������****************************/
/********************************************************************/
/* ������ ProcDec		 		                    */
/* ��  �� �����������ֵĴ���					    */
/* ����ʽ < ProcDec > ::=  �� |  ProcDeclaration                    */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ ProcDeclaration		 			    */
/* ��  �� �����������ֵĴ�����				    */
/* ����ʽ < ProcDeclaration > ::=  PROCEDURE ProcName(ParamList);   */
/*                                 ProcDecPart                      */
/*                                 ProcBody                         */
/*                                 ProcDecMore                      *
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ ProcDecMore    				            */
/* ��  �� ���ຯ�������д�����        	        	    */
/* ����ʽ < ProcDecMore > ::=  �� |  ProcDeclaration                */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ ParamList		 				    */
/* ��  �� ���������в����������ֵĴ�����	        	    */
/* ����ʽ < ParamList > ::=  �� |  ParamDecList                     */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ ParamDecList		 			    	    */
/* ��  �� ���������в����������ֵĴ�����	        	    */
/* ����ʽ < ParamDecList > ::=  Param  ParamMore                    */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ ParamMore		 			    	    */
/* ��  �� ���������в����������ֵĴ�����	        	    */
/* ����ʽ < ParamMore > ::=  �� | ; ParamDecList                    */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ Param		 			    	            */
/* ��  �� ���������в����������ֵĴ�����	        	    */
/* ����ʽ < Param > ::=  TypeName FormList | VAR TypeName FormList  */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ FormList		 			    	    */
/* ��  �� ���������в����������ֵĴ�����	        	    */
/* ����ʽ < FormList > ::=  id  FidMore                             */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ FidMore		 			    	    */
/* ��  �� ���������в����������ֵĴ�����	        	    */
/* ����ʽ < FidMore > ::=   �� |  , FormList                        */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ ProcDecPart		 			  	    */
/* ��  �� �����е��������ֵĴ�����	             	            */
/* ����ʽ < ProcDecPart > ::=  DeclarePart                          */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
/********************************************************************/
TreeNode ProcDecPart()
{
    TreeNode t = DeclarePart();
    return t;
}

/********************************************************************/
/* ������ ProcBody		 			  	    */
/* ��  �� �����岿�ֵĴ�����	                    	            */
/* ����ʽ < ProcBody > ::=  ProgramBody                             */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
/********************************************************************/
TreeNode ProcBody()
{
    TreeNode t = ProgramBody();
    if (t==null)
	syntaxError("a program body is requested!");
    return t;
}

/****************************�����岿��******************************/
/********************************************************************/
/********************************************************************/
/* ������ ProgramBody		 			  	    */
/* ��  �� �����岿�ֵĴ���	                    	            */
/* ����ʽ < ProgramBody > ::=  BEGIN  StmList   END                 */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ StmList		 			  	    */
/* ��  �� ��䲿�ֵĴ�����	                    	            */
/* ����ʽ < StmList > ::=  Stm    StmMore                           */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ StmMore		 			  	    */
/* ��  �� ��䲿�ֵĴ�����	                    	            */
/* ����ʽ < StmMore > ::=   �� |  ; StmList                         */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ Stm   		 			  	    */
/* ��  �� ��䲿�ֵĴ�����	                    	            */
/* ����ʽ < Stm > ::=   ConditionalStm   {IF}                       */
/*                    | LoopStm          {WHILE}                    */
/*                    | InputStm         {READ}                     */
/*                    | OutputStm        {WRITE}                    */
/*                    | ReturnStm        {RETURN}                   */
/*                    | id  AssCall      {id}                       */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ AssCall		 			  	    */
/* ��  �� ��䲿�ֵĴ�����	                    	            */
/* ����ʽ < AssCall > ::=   AssignmentRest   {:=,LMIDPAREN,DOT}     */
/*                        | CallStmRest      {(}                    */  
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ AssignmentRest		 			    */
/* ��  �� ��ֵ��䲿�ֵĴ�����	                    	    */
/* ����ʽ < AssignmentRest > ::=  VariMore : = Exp                  */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
/********************************************************************/
TreeNode AssignmentRest()
{
    TreeNode t = newStmtNode("AssignK");
	
    /* ��ֵ���ڵ�ĵ�һ�����ӽڵ��¼��ֵ��������������
    /* �ڶ������ӽ���¼��ֵ�����Ҳ���ʽ*/

    /*�����һ�����ӽ�㣬Ϊ�������ʽ���ͽڵ�*/
    TreeNode c = newExpNode("VariK");
    c.name[0] = temp_name;
    c.idnum = c.idnum+1;
    VariMore(c);
    t.child[0] = c;
		
    match("ASSIGN");
	  
    /*����ڶ������ӽڵ�*/
    t.child[1] = Exp(); 
				
    return t;
}

/********************************************************************/
/* ������ ConditionalStm		 			    */
/* ��  �� ������䲿�ֵĴ�����	                    	    */
/* ����ʽ <ConditionalStm>::=IF RelExp THEN StmList ELSE StmList FI */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ LoopStm          		 			    */
/* ��  �� ѭ����䲿�ֵĴ�����	                    	    */
/* ����ʽ < LoopStm > ::=   WHILE RelExp DO StmList ENDWH           */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ InputStm          		     	                    */
/* ��  �� ������䲿�ֵĴ�����	                    	    */
/* ����ʽ < InputStm > ::=  READ(id)                                */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ OutputStm          		     	                    */
/* ��  �� �����䲿�ֵĴ�����	                    	    */
/* ����ʽ < OutputStm > ::=   WRITE(Exp)                            */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ ReturnStm          		     	                    */
/* ��  �� ������䲿�ֵĴ�����	                    	    */
/* ����ʽ < ReturnStm > ::=   RETURN(Exp)                           */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
/********************************************************************/
TreeNode ReturnStm()
{
    TreeNode t = newStmtNode("ReturnK");
    match("RETURN");
    return t;
}

/********************************************************************/
/* ������ CallStmRest          		     	                    */
/* ��  �� ����������䲿�ֵĴ�����	                  	    */
/* ����ʽ < CallStmRest > ::=  (ActParamList)                       */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
/********************************************************************/
TreeNode CallStmRest()
{
    TreeNode t=newStmtNode("CallK");
    match("LPAREN");
    /*��������ʱ�����ӽڵ�ָ��ʵ��*/
    /*�������Ľ��Ҳ�ñ��ʽ���ͽ��*/
    TreeNode c = newExpNode("VariK"); 
    c.name[0] = temp_name;
    c.idnum = c.idnum+1;
    t.child[0] = c;
    t.child[1] = ActParamList();
    match("RPAREN");
    return t;
}

/********************************************************************/
/* ������ ActParamList          		   	            */
/* ��  �� ��������ʵ�β��ֵĴ�����	                	    */
/* ����ʽ < ActParamList > ::=     �� |  Exp ActParamMore           */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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
/* ������ ActParamMore          		   	            */
/* ��  �� ��������ʵ�β��ֵĴ�����	                	    */
/* ����ʽ < ActParamMore > ::=     �� |  , ActParamList             */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ�  */
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

/*************************���ʽ����********************************/
/*******************************************************************/
/* ������ Exp							   */
/* ��  �� ���ʽ������					   */
/* ����ʽ Exp ::= simple_exp | ��ϵ�����  simple_exp              */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ� */
/*******************************************************************/
TreeNode Exp()
{
    TreeNode t = simple_exp();

    /* ��ǰ����tokenΪ�߼����㵥��LT����EQ */
    if ((token.Lex.equals("LT"))||(token.Lex.equals("EQ"))) 
    {
        TreeNode p = newExpNode("OpK");

	/* ����ǰ����token(ΪEQ����LT)�����﷨���ڵ�p���������Աattr.op*/
	p.child[0] = t;
        p.attr.expAttr.op = token.Lex;
        t = p;
 
        /* ��ǰ����token��ָ���߼����������(ΪEQ����LT)ƥ�� */ 
        match(token.Lex);

        /* �﷨���ڵ�t�ǿ�,���ü򵥱��ʽ������simple_exp()	   
           ���������﷨���ڵ��t�ĵڶ��ӽڵ��Աchild[1]  */ 
        if (t!=null)
            t.child[1] = simple_exp();
    }
    return t;
}

/*******************************************************************/
/* ������ simple_exp						   */
/* ��  �� ���ʽ����						   */
/* ����ʽ simple_exp ::=   term  |  �ӷ������  term               */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ� */
/*******************************************************************/
TreeNode simple_exp()
{
    TreeNode t = term();

    /* ��ǰ����tokenΪ�ӷ����������PLUS��MINUS */
    while ((token.Lex.equals("PLUS"))||(token.Lex.equals("MINUS")))
    {
	TreeNode p = newExpNode("OpK");
	p.child[0] = t;
        p.attr.expAttr.op = token.Lex;
        t = p;

        match(token.Lex);

	/* ����Ԫ������term(),���������﷨���ڵ��t�ĵڶ��ӽڵ��Աchild[1] */
        t.child[1] = term();
    }
    return t;
}

/********************************************************************/
/* ������ term						            */
/* ��  �� �����						    */
/* ����ʽ < �� > ::=  factor | �˷������  factor		    */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ� */
/********************************************************************/
TreeNode term()
{
    TreeNode t = factor();

    /* ��ǰ����tokenΪ�˷����������TIMES��OVER */
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
/* ������ factor						     */
/* ��  �� ���Ӵ�����						     */
/* ����ʽ factor ::= INTC | Variable | ( Exp )                       */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ� */
/*********************************************************************/
TreeNode factor()
{
    TreeNode t = null;
    if (token.Lex.equals("INTC")) 
    {
        t = newExpNode("ConstK");

	/* ����ǰ������tokenStringת��Ϊ��������t����ֵ��Աattr.val */
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
/* ������ Variable						    */
/* ��  �� ����������						    */
/* ����ʽ Variable   ::=   id VariMore                   	    */
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ� */
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
/* ������ VariMore						    */
/* ��  �� ��������						    */
/* ����ʽ VariMore   ::=  ��                             	    */
/*                       | [Exp]            {[}                     */
/*                       | . FieldVar       {DOT}                   */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ� */
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
/* ������ FieldVar						    */
/* ��  �� ����������				                    */
/* ����ʽ FieldVar   ::=  id  FieldVarMore                          */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ� */
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
/* ������ FieldVarMore  			                    */
/* ��  �� ����������                                              */
/* ����ʽ FieldVarMore   ::=  ��| [Exp]            {[}              */ 
/* ˵  �� ���������ķ�����ʽ,������Ӧ�ĵݹ鴦����,�����﷨���ڵ� */
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
/* ������ match							    */
/* ��  �� �ռ���ƥ�䴦����				            */
/* ˵  �� ��������expected�����������ʷ����뵱ǰ���ʷ���token��ƥ�� */
/*        �����ƥ��,�򱨷����������﷨����			    */
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
/* ������ syntaxError                                       */
/* ��  �� �﷨��������		                    */
/* ˵  �� ����������messageָ���Ĵ�����Ϣ���               */	
/************************************************************/
void syntaxError(String s)     /*�������Ϣ.txt��д���ַ���*/
{
    serror=serror+"\n>>> ERROR :"+"Syntax error at "                              +String.valueOf(token.lineshow)+": "+s; 

    /* ���ô���׷�ٱ�־ErrorΪTRUE,��ֹ�����һ������ */
    Error = true;
}

/********************************************************************/
/* ������ ReadNextToken                                             */
/* ��  �� ��Token������ȡ��һ��Token				    */
/* ˵  �� ���ļ��д��Token����������ȡһ�����ʣ���Ϊ��ǰ����       */	
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
 *********�����Ǵ����﷨�����õĸ���ڵ������***********
 ********************************************************/
/********************************************************/
/* ������ newNode				        */	
/* ��  �� �����﷨���ڵ㺯��			        */
/* ˵  �� �ú���Ϊ�﷨������һ���µĽ��      	        */
/*        �����﷨���ڵ��Ա����ֵ�� sΪProcK, PheadK,  */
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
/* ������ newStmtNode					*/	
/* ��  �� ������������﷨���ڵ㺯��			*/
/* ˵  �� �ú���Ϊ�﷨������һ���µ�������ͽ��	*/
/*        �����﷨���ڵ��Ա��ʼ��			*/
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
/* ������ newExpNode					*/
/* ��  �� ���ʽ�����﷨���ڵ㴴������			*/
/* ˵  �� �ú���Ϊ�﷨������һ���µı��ʽ���ͽ��	*/
/*        �����﷨���ڵ�ĳ�Ա����ֵ			*/
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



