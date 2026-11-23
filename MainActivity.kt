package com.rohit.puneexpensemanager
import android.app.*
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
data class Expense(val id:Long,val category:String,val amount:Double,val note:String,val date:String)
class MainActivity:Activity(){
 private val green=Color.rgb(18,112,64); private val cats=listOf("Outgoing Bus","Metro","Breakfast","Lunch","Incoming Bus","Dinner","Additional expenses")
 private val prefs by lazy{getSharedPreferences("pune_expenses",0)}; private val items=mutableListOf<Expense>()
 private val today get()=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
 private val month get()=today.substring(0,7); private val budget=10000.0
 private lateinit var spent:TextView; private lateinit var remain:TextView; private lateinit var usage:TextView; private lateinit var bar:ProgressBar; private lateinit var history:LinearLayout
 private fun dp(x:Int)=(x*resources.displayMetrics.density).toInt()
 private fun label(s:String,n:Float,c:Int=Color.BLACK,b:Boolean=false)=TextView(this).apply{text=s;textSize=n;setTextColor(c);if(b)setTypeface(null,Typeface.BOLD)}
 override fun onCreate(saved:Bundle?){super.onCreate(saved);window.statusBarColor=Color.rgb(14,91,53);window.navigationBarColor=Color.rgb(14,91,53);load();screen();refresh()}
 private fun screen(){
  val scroll=ScrollView(this);val root=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(16),dp(16),dp(24));setBackgroundColor(Color.rgb(247,250,248))};scroll.addView(root);setContentView(scroll)
  val head=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(22),dp(18),dp(22));setBackgroundColor(green)}
  head.addView(label("PUNE EXPENSE MANAGER",22f,Color.WHITE,true));head.addView(label("Your Daily Money Tracker",14f,Color.WHITE));root.addView(head)
  val budgetCard=card();budgetCard.addView(label("MONTHLY BUDGET",13f,green,true));budgetCard.addView(label("₹10,000",30f,green,true))
  usage=label("Budget usage: 0%",13f);budgetCard.addView(usage);bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;progressTintList=android.content.res.ColorStateList.valueOf(green)};budgetCard.addView(bar,LinearLayout.LayoutParams(-1,dp(8)));root.addView(budgetCard)
  val row=LinearLayout(this).apply{orientation=0};val a=card();a.addView(label("TOTAL SPENT",12f,Color.DKGRAY,true));spent=label("₹0",23f,green,true);a.addView(spent)
  val b=card();b.addView(label("REMAINING",12f,Color.DKGRAY,true));remain=label("₹10,000",23f,green,true);b.addView(remain)
  row.addView(a,LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(5)});row.addView(b,LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(5)});root.addView(row)
  root.addView(Button(this).apply{text="+ ADD EXPENSE";setTextColor(Color.WHITE);setBackgroundColor(green);setOnClickListener{addDialog()}},LinearLayout.LayoutParams(-1,dp(52)))
  root.addView(label("Expense History",19f,Color.rgb(30,55,40),true).apply{setPadding(0,dp(22),0,dp(8))})
  history=LinearLayout(this).apply{orientation=1};root.addView(history)
  root.addView(label("Saved on this phone • Offline mode",12f,Color.GRAY).apply{gravity=Gravity.CENTER;setPadding(0,dp(18),0,0)})
 }
 private fun card()=LinearLayout(this).apply{orientation=1;setPadding(dp(14),dp(14),dp(14),dp(14));setBackgroundColor(Color.WHITE);elevation=dp(2).toFloat();layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(10),0,dp(5))}}
 private fun addDialog(){
  val box=LinearLayout(this).apply{orientation=1;setPadding(dp(20),0,dp(20),0)}
  val sp=Spinner(this);sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,cats)
  val amt=EditText(this).apply{hint="Amount (₹)";inputType=8194};val note=EditText(this).apply{hint="Note (optional)"}
  box.addView(label("Category",14f,green,true));box.addView(sp);box.addView(label("Amount",14f,green,true));box.addView(amt);box.addView(note)
  val d=AlertDialog.Builder(this).setTitle("Add expense").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create()
  d.setOnShowListener{d.getButton(-1).setOnClickListener{val v=amt.text.toString().toDoubleOrNull();if(v==null||v<=0){amt.error="Enter a valid amount"}else{items.add(0,Expense(System.currentTimeMillis(),cats[sp.selectedItemPosition],v,note.text.toString(),today));save();refresh();d.dismiss()}}};d.show()
 }
 private fun refresh(){
  val total=items.filter{it.date.startsWith(month)}.sumOf{it.amount};val left=budget-total
  spent.text=money(total);remain.text=money(left);remain.setTextColor(if(left<0)Color.RED else green)
  val p=(total/budget*100).toInt().coerceAtLeast(0);usage.text="Budget usage: $p%"+if(total>=9000)" • Budget alert" else "";bar.progress=p.coerceAtMost(100)
  history.removeAllViews()
  if(items.isEmpty())history.addView(label("No expenses yet. Tap + ADD EXPENSE to begin.",14f,Color.GRAY))
  items.sortedByDescending{it.id}.forEach{e->
   val r=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(10),dp(5),dp(10));setBackgroundColor(Color.WHITE)}
   val details=LinearLayout(this).apply{orientation=1};details.addView(label(e.category,15f,Color.BLACK,true));details.addView(label(e.date+(if(e.note.isNotBlank())" • "+e.note else ""),12f,Color.GRAY))
   r.addView(details,LinearLayout.LayoutParams(0,-2,1f));r.addView(label(money(e.amount),15f,green,true))
   r.addView(label("  ✕  ",17f,Color.RED,true).apply{setOnClickListener{AlertDialog.Builder(this@MainActivity).setMessage("Delete this expense?").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->items.removeAll{it.id==e.id};save();refresh()}.show()}})
   history.addView(r);history.addView(View(this).apply{setBackgroundColor(Color.rgb(238,248,241))},LinearLayout.LayoutParams(-1,dp(1)))
  }
 }
 private fun money(v:Double)="₹"+String.format(Locale.US,"%,.2f",v)
 private fun save(){val a=JSONArray();items.forEach{a.put(JSONObject().put("id",it.id).put("category",it.category).put("amount",it.amount).put("note",it.note).put("date",it.date))};prefs.edit().putString("items",a.toString()).apply()}
 private fun load(){try{val a=JSONArray(prefs.getString("items","[]"));for(i in 0 until a.length()){val o=a.getJSONObject(i);items.add(Expense(o.getLong("id"),o.getString("category"),o.getDouble("amount"),o.optString("note"),o.getString("date")))}}catch(_:Exception){}}
}
