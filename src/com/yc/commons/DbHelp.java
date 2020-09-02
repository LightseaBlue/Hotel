package com.yc.commons;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.sql.BLOB;


public class DbHelp {
	private Connection conn;
	private PreparedStatement pstmt;
	private ResultSet rs;
//	private static String driverName="oracle.jdbc.driver.OracleDriver";
//	private String url="jdbc:oracle:thin:@localhost:1521:orcl";
//	private String user="scott";
//	private String password="lw";
	//加载驱动
	static{
		try {
//			Class.forName(driverName);
			Class.forName(MyProperties.getInstance().getProperty("driverName"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	//获取数据库的连接
	public Connection getConn() throws SQLException{
//		conn=DriverManager.getConnection(url,user,password);
		conn=DriverManager.getConnection(MyProperties.getInstance().getProperty("url"),MyProperties.getInstance());
		return conn;
	}
	//关闭资源
	public void closeAll(Connection conn,PreparedStatement pstmt,ResultSet rs){
		
		if(null!=rs){
			try {
				rs.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(null!=pstmt){
			try {
				pstmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(null!=conn){
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}
	/**
	 *单记录查询 select * from table_name where id=? 
	 *传入的参数 集合中 集合的参数顺序必须和？顺序一致
	 * @throws SQLException 
	 *
	 */
	public Map<String, Object> selectSingle(String sql,List<Object> params) throws Exception{
		Map<String, Object> map=null;
		
	
		try {
			conn=getConn();
			pstmt=conn.prepareStatement(sql);
			//设置参数
			setParamsList(pstmt, params);
			//获取结果集
			rs=pstmt.executeQuery();
			//根据结果集对象获取到所有结果集中所有列名
			List<String> columnNames=getAllColumnName(rs);
			if(rs.next()){
				map=new HashMap<String,Object>();
				String typeName=null;//值的类型
				Object obj=null;  //获取的值
				//循环所有的列名
				for(String name:columnNames){
					obj=rs.getObject(name);
					if(null!=obj){
						typeName=obj.getClass().getName();
					}else{
						continue;
					}
					if("oracle.sql.BLOB".equals(typeName)){
						//对图片进行处理
						BLOB blob=(BLOB)obj;
						InputStream in=blob.getBinaryStream();//将此 Blob实例指定的 BLOB值作为流 Blob 。 
						byte []bt=new byte[(int)blob.length()];
						in.read(bt);
						map.put(name, bt);//将blob类型值以字节数组形式存储
						
					}else{
						map.put(name, obj);
					}
				}
			}
		}  finally{
			closeAll(conn, pstmt, rs);
		}
		return map;
	}
	
	/**
	 * 返回多条记录查询操作 select * from table_name
	 * @throws SQLException 
	 */
	public List<Map<String, Object>> selectMutil(String sql,List<Object> params) throws Exception{
		List<Map<String, Object>> list=new ArrayList<Map<String,Object>>();
		Map<String, Object> map=null;
		try{
			conn=getConn();
			pstmt=conn.prepareStatement(sql);
			//设置参数
			setParamsList(pstmt, params);
			//获取结果集
			rs=pstmt.executeQuery();
			//根据结果集对象获取到所有集合中所有列名
			List<String> columnNames=getAllColumnName(rs);
			while(rs.next()){
				map=new HashMap<String,Object>();
				String typeName=null;//值的类型
				Object obj=null;  //获取的值
				//循环所有的列名
				for(String name:columnNames){
					obj=rs.getObject(name);
					if(null!=obj){
						typeName=obj.getClass().getName();	
					}else{
						continue;
					}
					if("oracle.sql.BLOB".equals(typeName)){
						//对图片进行处理
						BLOB blob=(BLOB)obj;
						InputStream in=blob.getBinaryStream();
						byte []bt=new byte[(int)blob.length()];
						in.read(bt);
						map.put(name, bt);//将blob类型值以字节数组形式存储
						
					}else{
						map.put(name, obj);
					}
				}
				list.add(map);
			}
		}finally{
			closeAll(conn, pstmt, rs);
		}
		return list;
	}
	/**
	 * 获取查询后的字段名
	 * @throws SQLException 
	 */
	public List<String> getAllColumnName(ResultSet rs) throws SQLException{
		List<String> list=new ArrayList<String>();
		//ResultSetMetaData:可用于获取有关ResultSet对象中列的类型和属性的信息的对象
		ResultSetMetaData data=rs.getMetaData();
		int count=data.getColumnCount();
		for(int i=1;i<=count;i++){
			String str=data.getColumnName(i);//获取指定列的列名
			//添加列名到List集合中
			list.add(str);
		}
		return list;
	}
 //将集合设置到预编译对象中
	public void setParamsList(PreparedStatement pstmt,List<Object> params) throws SQLException{
		if(null==params || params.size()<=0){
			return;
		}
		for(int i=0;i<params.size();i++){
			pstmt.setObject(i+1, params.get(i));
		}
	}
	 /**
	  * 批处理操作  多个 insert update delect 同一事务
	  * @param sql  多条SQL语句
	  * @param params  多条sql语句的参数  每条sql语句参数在小List集合中，多个再封装到大的List集合  一一对应
	  * @return
	  */
	public int update(List<String> sqls,List<List<Object>> params)throws SQLException{
		int result=0;
		try {
			conn=getConn();
			//设置事务手动提交
			conn.setAutoCommit(false);
			//循环sql语句
			if(null==sqls ||sqls.size()<=0){
			return result; 
			}
			for(int i=0;i<params.size();i++){
				//获取sql语句并创建预编译对象
				pstmt=conn.prepareStatement(sqls.get(i));
				//获取对应的sql语句参数集合
				List<Object> param=params.get(i);
				//设置参数
				setParamsList(pstmt, param);
				//执行更新
				result=pstmt.executeUpdate();
				if(result<=0){
					return result;
				}
			}
			//手动提交
			conn.commit();
		} catch (Exception e) {
			//设置回滚
			conn.rollback();
			result=0;
		}finally{
			//还原事务的状态
			conn.setAutoCommit(true);
			closeAll(conn, pstmt, rs);
		}
		
		return result;
	}
	
	/**
	 * @throws SQLException 
	 * 更新操作 增删改
	 * 传入的参数  不定长的对象数组 传入的参数顺序必须和？顺序一致
	 *  
	 */
	
	public int update(String sql,Object...params) throws SQLException{
		int result=0;
		try {
			conn=getConn();//获取连接对象
			pstmt=conn.prepareStatement(sql);
			//设置参数
			setParamsObject(pstmt, params);
			//执行
			result=pstmt.executeUpdate();
		} finally{
			closeAll(conn, pstmt, null);
		}
		return result;
	}
	//不定长参数   设置参数  传入的参数顺序必须和？顺序一致
	public void setParamsObject(PreparedStatement pstme,Object...params) throws SQLException{
		if(null==params ||params.length<=0){
			return;
		}
		for(int i=0;i<params.length;i++){
			//setObject():使用给定对象设置指定参数的值
		pstmt.setObject(i+1, params[i]); //将数组中的第i个元素值设置为第i+1个问号
		}
	}
	/**
	 * 聚合函数操作
	 * @param sql
	 * @param params
	 * @return
	 * @throws SQLException
	 */
	public double getPloymer(String sql,List<Object> params)throws SQLException{
		double result=0;
		try {
			conn=getConn();
			pstmt=conn.prepareStatement(sql);
			setParamsList(pstmt, params);
			rs=pstmt.executeQuery();
			if(rs.next()){
				result=rs.getDouble(1);
			}
		} finally{
			closeAll(conn, pstmt, rs);
			
		};
		
		return result;
	}
	////////////////////////////////////////////////////////////////////////////////////////////
	public Map<String, Object> selectSingle1(String sql,List<Map<String, Object>> params) throws Exception{
		Map<String, Object> map=null;
		
	
		try {
			conn=getConn();
			pstmt=conn.prepareStatement(sql);
			//设置参数
			setParamsList1(pstmt, params);
			//获取结果集
			rs=pstmt.executeQuery();
			//根据结果集对象获取到所有结果集中所有列名
			List<String> columnNames=getAllColumnName(rs);
			if(rs.next()){
				map=new HashMap<String,Object>();
				String typeName=null;//值的类型
				Object obj=null;  //获取的值
				//循环所有的列名
				for(String name:columnNames){
					obj=rs.getObject(name);
					if(null!=obj){
						typeName=obj.getClass().getName();
					}
					if("oracle.sql.BLOB".equals(typeName)){
						//对图片进行处理
						BLOB blob=(BLOB)obj;
						InputStream in=blob.getBinaryStream();//将此 Blob实例指定的 BLOB值作为流 Blob 。 
						byte []bt=new byte[(int)blob.length()];
						in.read(bt);
						map.put(name, bt);//将blob类型值以字节数组形式存储
						
					}else{
						map.put(name, obj);
					}
				}
			}
		}  finally{
			closeAll(conn, pstmt, rs);
		}
		return map;
	}
	public void setParamsList1(PreparedStatement pstmt,List<Map<String, Object>> params) throws SQLException{
		if(null==params || params.size()<=0){
			return;
		}
		for(int i=0;i<params.size();i++){
			pstmt.setObject(i+1, params.get(i));
		}
	}
	}
