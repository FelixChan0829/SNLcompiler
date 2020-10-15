import java.io.*;
import java.util.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import mycompiler.cifa.*;
import mycompiler.yufa.*;
import mycompiler.yuyi.*;
import mycompiler.zhongjian.*;
import mycompiler.youhua.*;
import mycompiler.youhua.changliang.*;
import mycompiler.youhua.gonggong.*;
import mycompiler.youhua.xunhuan.*;
import mycompiler.mubiao.*;
import mycompiler.jieshiqi.*;

public class compiler extends Applet implements ActionListener
{
    Button button1,button2,button3,button4,button5,button6;
    Button ybutton1,ybutton2,ybutton3,ybutton4;
    Label label1,label2;
    TextArea text1;
    MyWindow window1,window2,window3,window4,window5,window6,window7;
    MyWindow ywindow1,ywindow2,ywindow3,ywindow4;
    GridLayout net;

    public void init()
    {
	net=new GridLayout(1,2);
        setLayout(net);

	text1=new TextArea("",100,400);
        Panel p1=new Panel();
	p1.setLayout(new GridLayout(1,1));
        p1.add(text1);
        add(p1);

	label1=new Label("在左边输入程序，按键看结果");
	label2=new Label("中间代码优化");
	button1=new Button("词法分析");
	button2=new Button("语法分析");
	button3=new Button("语义分析");
	button4=new Button("中间代码生成");
        ybutton1=new Button("常量表达式优化");
	ybutton2=new Button("公共表达式优化");
	ybutton3=new Button("循环不变表达式外提");
	ybutton4=new Button("三种优化后的结果");
	button5=new Button("目标代码生成");
	button6=new Button("执行结果");
        Panel p2=new Panel();
	p2.setLayout(new GridLayout(8,2));
        p2.add(label1);    p2.add(new Label());        
        p2.add(button1);   p2.add(button2);        
        p2.add(button3);   p2.add(button4);
        p2.add(label2);    p2.add(new Label());
        p2.add(ybutton1);  p2.add(ybutton2);        
        p2.add(ybutton3);  p2.add(ybutton4); 
        p2.add(new Label());    p2.add(new Label());                 
        p2.add(button5);   p2.add(button6);
        add(p2);

	window1=new MyWindow("词法分析");
	window2=new MyWindow("语法树");
	window3=new MyWindow("语法错误信息");
	window4=new MyWindow("符号表");
	window5=new MyWindow("语义错误信息");
	window6=new MyWindow("四元式");
	window7=new MyWindow("目标代码");
	ywindow1=new MyWindow("常量表达式优化结果");
	ywindow2=new MyWindow("公共表达式优化结果");
	ywindow3=new MyWindow("循环不变表达式外提结果");
	ywindow4=new MyWindow("三种优化后的结果");

	button1.addActionListener(this);
	button2.addActionListener(this);
	button3.addActionListener(this);
	button4.addActionListener(this);
	button5.addActionListener(this);
	button6.addActionListener(this);
	ybutton1.addActionListener(this);
	ybutton2.addActionListener(this);
	ybutton3.addActionListener(this);
	ybutton4.addActionListener(this);
    }
    public void actionPerformed(ActionEvent e)
    {
	if (e.getSource()==button1)
	{
           String s=text1.getText();
           CreatToken ct=new CreatToken(s); 
           s=ct.tok; 
           s=s.trim();         
           window1.text.setText(s);
	   window1.setVisible(true); 
	}
        else if (e.getSource()==button2)
	{
           String s=text1.getText();
           CreatToken ct=new CreatToken(s); 
           s=ct.tok; 
           s=s.trim();
           Recursion r=new Recursion(s);
           if (r.Error)
           {
               window3.text.setText(r.serror);
	       window3.setVisible(true);
           } 
           else
           {
               window2.text.setText(r.stree);
	       window2.setVisible(true);
           } 
	}
        else if (e.getSource()==button3)
	{
           String s=text1.getText();
           CreatToken ct=new CreatToken(s); 
           s=ct.tok; 
           s=s.trim();
	   AnalYuyi a=new AnalYuyi(s);
           if (a.Error1)
           {
               window5.text.setText(a.serror);
	       window5.setVisible(true);
           } 
           else if (a.Error)
           {
               window3.text.setText(a.yerror);
	       window3.setVisible(true);
           }
           else
           {
               window4.text.setText(a.ytable);
	       window4.setVisible(true);
           } 
	}
        else if (e.getSource()==button4)
	{
           String s=text1.getText();
           CreatToken ct=new CreatToken(s); 
           s=ct.tok; 
           s=s.trim();
           Midcode m=new Midcode(s);
           if (m.Error1)
           {
               window5.text.setText(m.serror);
	       window5.setVisible(true);
           } 
           else if (m.Error)
           {
               window3.text.setText(m.yerror);
	       window3.setVisible(true);
           }
           else
           {
               window6.text.setText(m.midcode);
	       window6.setVisible(true); 
           } 
	}
        else if (e.getSource()==ybutton1)
        {
           String s=text1.getText();
           CreatToken ct=new CreatToken(s); 
           s=ct.tok; 
           s=s.trim();
           Const c=new Const(s);
           if (c.Error1)
           {
               window5.text.setText(c.serror);
	       window5.setVisible(true);
           } 
           else if (c.Error)
           {
               window3.text.setText(c.yerror);
	       window3.setVisible(true);
           }
           else
           {
               ywindow1.text.setText(c.midcode);
	       ywindow1.setVisible(true); 
           } 
       }
       else if (e.getSource()==ybutton2)
       {
           String s=text1.getText();
           CreatToken ct=new CreatToken(s); 
           s=ct.tok; 
           s=s.trim();
           Gongg g=new Gongg(s);
           if (g.Error1)
           {
               window5.text.setText(g.serror);
	       window5.setVisible(true);
           } 
           else if (g.Error)
           {
               window3.text.setText(g.yerror);
	       window3.setVisible(true);
           }
           else
           {
               ywindow2.text.setText(g.midcode);
	       ywindow2.setVisible(true); 
           } 
       }
       else if (e.getSource()==ybutton3)
       {
           String s=text1.getText();
           CreatToken ct=new CreatToken(s); 
           s=ct.tok; 
           s=s.trim();
           Xunh x=new Xunh(s);
           if (x.Error1)
           {
               window5.text.setText(x.serror);
	       window5.setVisible(true);
           } 
           else if (x.Error)
           {
               window3.text.setText(x.yerror);
	       window3.setVisible(true);
           }
           else
           {
               ywindow3.text.setText(x.midcode);
	       ywindow3.setVisible(true); 
           } 
       }
       else if (e.getSource()==ybutton4)
       {
           String s=text1.getText();
           CreatToken ct=new CreatToken(s); 
           s=ct.tok; 
           s=s.trim();
           Opt o=new Opt(s);
           if (o.Error1)
           {
               window5.text.setText(o.serror);
	       window5.setVisible(true);
           } 
           else if (o.Error)
           {
               window3.text.setText(o.yerror);
	       window3.setVisible(true);
           }
           else
           {
               ywindow4.text.setText(o.midcode);
	       ywindow4.setVisible(true); 
           } 
	}
        else if (e.getSource()==button5)
	{
           String s=text1.getText();
           CreatToken ct=new CreatToken(s); 
           s=ct.tok; 
           s=s.trim();
           Target t=new Target(s);
           if (t.Error1)
           {
               window5.text.setText(t.serror);
	       window5.setVisible(true);
           } 
           else if (t.Error)
           {
               window3.text.setText(t.yerror);
	       window3.setVisible(true);
           }
           else
           {
               window7.text.setText(t.mbcode);
	       window7.setVisible(true); 
           }
	}
        else if (e.getSource()==button6)
	{
           String s=text1.getText();
           CreatToken ct=new CreatToken(s); 
           s=ct.tok; 
           s=s.trim();
           Translator tt=new Translator(s);
           if (tt.Error1)
           {
               window5.text.setText(tt.serror);
	       window5.setVisible(true);
           } 
           else if (tt.Error)
           {
               window3.text.setText(tt.yerror);
	       window3.setVisible(true);
           }
        }        
    }
}
class MyWindow extends Frame
{
    TextArea text;
    MyWindow(String s)
    {
	super(s);
	setLayout(new GridLayout(1,1));
        text=new TextArea("",100,300);
        add(text);
	setVisible(false);
	pack();
        addWindowListener(new WindowAdapter() 
        {
            public void windowClosing(WindowEvent e)
            {setVisible(false); System.exit(0);}  
        });
    }
}




