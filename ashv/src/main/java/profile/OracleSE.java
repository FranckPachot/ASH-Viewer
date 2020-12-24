package profile;

import java.util.LinkedList;
import java.util.List;

public class OracleSE extends OracleEE implements IProfile {
    String profileName = "OracleSE";

    private String sqlTextAsh = "SELECT * FROM (SELECT sysdate SAMPLE_TIME, vs.sid SESSION_ID, vs.state SESSION_STATE,\n"+
"vs.serial# SESSION_SERIAL#, vs.user# USER_ID, vs.sql_id SQL_ID, vs.type SESSION_TYPE,\n"+
"vs.event# EVENT#, (CASE WHEN vs.wait_time != 0 THEN 'CPU used' ELSE vs.event END) EVENT,\n"+
"vs.seq# SEQ#, vs.p1 P1, vs.p2 P2, vs.p3 P3,\n"+
"vs.wait_time WAIT_TIME, vs.wait_class_id WAIT_CLASS_ID, vs.wait_class# WAIT_CLASS#,\n"+
"(CASE WHEN vs.wait_time != 0 THEN 'CPU used' ELSE vs.wait_class END) WAIT_CLASS, vss.value TIME_WAITED,\n"+
"vs.row_wait_obj# CURRENT_OBJ#, vs.row_wait_file# CURRENT_FILE#, vs.row_wait_block# CURRENT_BLOCK#,\n"+
"vs.program PROGRAM, vs.module MODULE, vs.action ACTION, vs.fixed_table_sequence FIXED_TABLE_SEQUENCE,\n"+
"nvl(au.name, 'UNKNOWN') COMMAND\n"+
"FROM\n"+
"v$session vs, v$sesstat vss, audit_actions au\n"+
"WHERE vs.sid != ( select distinct sid from v$mystat  where rownum < 2 )\n"+
"and vs.sid = vss.sid and vs.command = au.action(+)\n"+
"and vss.statistic# = 12 and (vs.wait_class != 'Idle' or vs.wait_time != 0)\n"+
"union all\n"+
"select \n"+
"SAMPLE_TIME - offset/24/60/60 ,dense_rank()over(partition by sample_time,WAIT_CLASS,EVENT#,EVENT order by aas desc) SESSION_ID,SESSION_STATE,SESSION_SERIAL#,0 USER_ID,'' SQL_ID,'STATSPACK' SESSION_TYPE,EVENT#,EVENT\n"+
",null,null,null,null,null,null,null\n"+
",WAIT_CLASS,null,null,null,null,null,MODULE,ACTION,null,null\n"+
"from (\n"+
"select /*+ result_cache */\n"+
"snap_time SAMPLE_TIME\n"+
",SESSION_STATE,WAIT_CLASS,EVENT#,EVENT,snap_id SESSION_SERIAL#\n"+
",   'SNAP_ID='||snap_id||' ela='||dbms_xplan.format_time_s((snap_time-lag(snap_time) over (partition by dbid,instance_number,SESSION_STATE,WAIT_CLASS,EVENT#,EVENT order by snap_id))*24*60*60) MODULE\n"+
",   dbms_xplan.format_time_s((value-lag(value)         over (partition by dbid,instance_number,SESSION_STATE,WAIT_CLASS,EVENT#,EVENT order by snap_id))/1e6)||'s' ACTION\n"+
",case when snap_time>lag(snap_time) over (partition by dbid,instance_number,SESSION_STATE,WAIT_CLASS,EVENT#,EVENT order by snap_id) then\n"+
"          (value-lag(value)         over (partition by dbid,instance_number,SESSION_STATE,WAIT_CLASS,EVENT#,EVENT order by snap_id)) / 1e6 / 60 / 60 / 24\n"+
"        / (snap_time-lag(snap_time) over (partition by dbid,instance_number,SESSION_STATE,WAIT_CLASS,EVENT#,EVENT order by snap_id))\n"+
" end aas\n"+
"from \n"+
"(\n"+
"select dbid,instance_number,snap_id,'WAITING'  SESSION_STATE,           WAIT_CLASS, name      EVENT, event# EVENT#,(time_waited_micro) value \n"+
"from stats$system_event join v$event_name using(event_id) where wait_class<>'Idle'\n"+
"union all\n"+
"select dbid,instance_number,snap_id,'CPU used' SESSION_STATE,'CPU used' WAIT_CLASS, 'CPU used' EVENT,0      EVENT#,value \n"+
"from stats$sys_time_model join stats$time_model_statname using (stat_id) where stat_name in ('DB CPU') \n"+
") join stats$snapshot using(dbid,instance_number,snap_id)\n"+
") aas join (select rownum/4 num from xmltable('1 to 10000')) ten on (ten.num <= aas.aas)\n"+
", ( select rownum offset from xmltable('1 to 5')\n"+
")  )\n"+
"order by 1 desc\n";


    long interval = 1000; // 1 sec

    List<String> sqlIdAdditionalColName = new LinkedList<>();

    public OracleSE() {
        sqlIdAdditionalColName.add("COMMAND");
    }

    @Override
    public String getProfileName() { return profileName; }

    @Override
    public String getSqlTextAsh() { return sqlTextAsh; }

    @Override
    public String getSqlTextAshOneRow() { return sqlTextAsh; }

    @Override
    public List<String> getSqlIdAdditionalColName() { return sqlIdAdditionalColName; }

    @Override
    public long getInterval() {
        return interval;
    }
}
