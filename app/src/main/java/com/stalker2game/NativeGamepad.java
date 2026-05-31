package com.stalker2game;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;
import org.microemu.MIDletAccess;
import org.microemu.MIDletBridge;
import org.microemu.DisplayAccess;

public class NativeGamepad extends View {
    private static final int UP=-1,DOWN=-2,LEFT=-3,RIGHT=-4,FIRE=-5,SOFT1=-6,SOFT2=-7;
    private static final int K0=48,K7=55,KSTAR=42,KPOUND=35;
    private Vibrator vib;
    private float W,H,dCX,dCY,dR,aBX,aBY,aBR;
    private RectF sL,sR,b0,b7,bS,bP;
    private final boolean[] pressed=new boolean[11];
    private final Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hl=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tx=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ar=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] KEYS={UP,DOWN,LEFT,RIGHT,FIRE,SOFT1,SOFT2,K0,K7,KSTAR,KPOUND};

    public NativeGamepad(Context c){
        super(c);
        vib=(Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);
        bg.setColor(Color.argb(160,20,20,40)); bg.setStyle(Paint.Style.FILL);
        hl.setColor(Color.argb(220,0,160,220)); hl.setStyle(Paint.Style.FILL);
        tx.setColor(Color.WHITE); tx.setTextAlign(Paint.Align.CENTER); tx.setTypeface(Typeface.DEFAULT_BOLD);
        ar.setColor(Color.argb(200,255,255,255)); ar.setTextAlign(Paint.Align.CENTER); ar.setTypeface(Typeface.DEFAULT_BOLD);
    }

    @Override protected void onSizeChanged(int w,int h,int ow,int oh){
        W=w; H=h;
        dR=W*0.19f; dCX=W*0.21f; dCY=H*0.79f;
        aBR=W*0.11f; aBX=W*0.82f; aBY=H*0.75f;
        float bh=H*0.06f,by=H*0.88f;
        sL=new RectF(W*0.22f,by,W*0.48f,by+bh);
        sR=new RectF(W*0.52f,by,W*0.78f,by+bh);
        float aw=W*0.22f,ah=H*0.055f,ay=H*0.935f;
        b0=new RectF(W*0.01f,ay,W*0.01f+aw,ay+ah);
        b7=new RectF(W*0.26f,ay,W*0.26f+aw,ay+ah);
        bS=new RectF(W*0.51f,ay,W*0.51f+aw,ay+ah);
        bP=new RectF(W*0.76f,ay,W*0.76f+aw,ay+ah);
        tx.setTextSize(H*0.022f); ar.setTextSize(dR*0.52f);
    }

    @Override protected void onDraw(android.graphics.Canvas c){
        c.drawCircle(dCX,dCY,dR,bg);
        if(pressed[0]) drawSeg(c,0); if(pressed[1]) drawSeg(c,180);
        if(pressed[2]) drawSeg(c,270); if(pressed[3]) drawSeg(c,90);
        c.drawCircle(dCX,dCY,dR*0.26f,pressed[4]?hl:bg);
        float ar2=dR*0.63f;
        drawArrow(c,"▲",dCX,dCY-ar2,pressed[0]);
        drawArrow(c,"▼",dCX,dCY+ar2,pressed[1]);
        drawArrow(c,"◀",dCX-ar2,dCY,pressed[2]);
        drawArrow(c,"▶",dCX+ar2,dCY,pressed[3]);
        c.drawCircle(aBX,aBY,aBR,pressed[4]?hl:bg);
        tx.setTextSize(aBR*0.42f);
        c.drawText("OK",aBX,aBY+tx.getTextSize()*0.4f,tx);
        tx.setTextSize(H*0.022f);
        drawBtn(c,sL,"MENU",5); drawBtn(c,sR,"BACK",6);
        tx.setTextSize(H*0.018f);
        drawBtn(c,b0,"MAP",7); drawBtn(c,b7,"DROP",8);
        drawBtn(c,bS,"SQUAD",9); drawBtn(c,bP,"LOG",10);
    }

    private void drawSeg(android.graphics.Canvas c,float ang){
        Path p=new Path(); p.moveTo(dCX,dCY);
        p.arcTo(new RectF(dCX-dR,dCY-dR,dCX+dR,dCY+dR),ang-45,90); p.close();
        c.drawPath(p,hl);
    }
    private void drawArrow(android.graphics.Canvas c,String s,float x,float y,boolean on){
        ar.setAlpha(on?255:170);
        c.drawText(s,x,y+ar.getTextSize()*0.4f,ar);
    }
    private void drawBtn(android.graphics.Canvas c,RectF r,String s,int i){
        c.drawRoundRect(r,14,14,pressed[i]?hl:bg);
        tx.setAlpha(pressed[i]?255:200);
        c.drawText(s,r.centerX(),r.centerY()+tx.getTextSize()*0.4f,tx);
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        boolean[] np=new boolean[11];
        int act=e.getActionMasked();
        if(act!=MotionEvent.ACTION_UP&&act!=MotionEvent.ACTION_CANCEL){
            for(int i=0;i<e.getPointerCount();i++) check(e.getX(i),e.getY(i),np);
        }
        for(int i=0;i<11;i++){
            if(np[i]&&!pressed[i]){fire(i,true);buzz();}
            else if(!np[i]&&pressed[i]) fire(i,false);
            pressed[i]=np[i];
        }
        invalidate(); return true;
    }

    private void check(float x,float y,boolean[] p){
        float dx=x-dCX,dy=y-dCY,d=(float)Math.sqrt(dx*dx+dy*dy);
        if(d<dR){
            if(d<dR*0.3f){p[4]=true;return;}
            float a=(float)Math.toDegrees(Math.atan2(dy,dx));
            if(a<0)a+=360;
            if(a>=315||a<45)p[3]=true;
            else if(a<135)p[1]=true;
            else if(a<225)p[2]=true;
            else p[0]=true;
            return;
        }
        float ad=(float)Math.sqrt((x-aBX)*(x-aBX)+(y-aBY)*(y-aBY));
        if(ad<aBR*1.4f){p[4]=true;return;}
        if(sL!=null&&sL.contains(x,y)){p[5]=true;return;}
        if(sR!=null&&sR.contains(x,y)){p[6]=true;return;}
        if(b0!=null&&b0.contains(x,y)){p[7]=true;return;}
        if(b7!=null&&b7.contains(x,y)){p[8]=true;return;}
        if(bS!=null&&bS.contains(x,y)){p[9]=true;return;}
        if(bP!=null&&bP.contains(x,y))p[10]=true;
    }

    private void fire(int i,boolean dn){
        if(i>=KEYS.length) return;
        // Use DisplayAccess.keyPressed() - the correct public API
        MIDletAccess ma=MIDletBridge.getMIDletAccess();
        if(ma==null) return;
        DisplayAccess da=ma.getDisplayAccess();
        if(da==null) return;
        if(dn) da.keyPressed(KEYS[i]);
        else   da.keyReleased(KEYS[i]);
    }

    private void buzz(){
        if(vib!=null&&vib.hasVibrator())
            vib.vibrate(VibrationEffect.createOneShot(12,VibrationEffect.DEFAULT_AMPLITUDE));
    }
}
