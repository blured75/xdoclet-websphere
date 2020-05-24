/*
 * Copyright (c) 2004 The XDoclet team
 * All rights reserved.
 *
 */ 
package xdoclet.modules.ibm.websphere.ejb;

import java.io.InputStream;
import java.util.*;

import org.apache.commons.logging.Log;

import xdoclet.XDocletException;
import xdoclet.modules.ejb.EjbTagsHandler;
import xdoclet.modules.ejb.XDocletModulesEjbMessages;
import xdoclet.modules.ejb.dd.RelationTagsHandler;
import xdoclet.util.LogUtil;
import xdoclet.util.Translator;
import xdoclet.util.TypeConversionUtil;
import xjavadoc.*;

/**
 * @author Ken Hygh (kenhygh@nc.rr.com), Denis Boutin (denisboutin@free.fr)
 * @created: Jan 3, 2004
 * @xdoclet.taghandler namespace="WASEjb"
 */
public class WasEjbTagsHandler extends RelationTagsHandler {
	
 

	private final static String WEBSPHERE_MAPPINGS_FILE_NAME = "xdoclet/modules/ibm/websphere/ejb/resources/typeMappings.properties";
	
	/**
	 * TODO
	 * 
	 * 
	 * @throws XDocletException
	 */
	public WasEjbTagsHandler() throws XDocletException {
		loadMappings();
	}

	/**
	 * TODO
	 * 
	 * 
	 * @param p
	 * @return
	 * @throws XDocletException
	 */
	public String getJDBCType(Properties p) throws XDocletException {
		String dbName = getGeneralDBName((String)p.get("dbType"));
		String jdbcType = getGeneralJDBCType((String)p.get("jdbcType"));
		
		System.out.println("mappings.get(" + dbName + ".JDBC." + jdbcType + ")");
		String result = (String)mappings.get(dbName + ".JDBC." + jdbcType);
		if (result == null || "".equals(result))
			result = "CHAR"; // a dumb but plausible default.
		
		System.out.println("getJDBCType() for " + getCurrentMethod().getName() + " " + dbName + ":" + jdbcType + " returns " + result);
		
		return result;
	}
	
	/**
	 * 
	 * TODO
	 * 
	 * @param p
	 * @return
	 * @throws XDocletException
	 */
	public String getSQLType(Properties p) throws XDocletException {
		String dbName = getGeneralDBName((String)p.get("dbType"));
		String sqlType = getGeneralJDBCType((String)p.get("sqlType"));
		String result = (String)mappings.get(dbName + ".SQL." + sqlType);
		System.out.println("getSqlType(" + p.toString() + ")");
		System.out.println("mapping.get(" + dbName + ".SQL." + sqlType + ")");
		System.out.println("==> " + result);
		if (result == null || "".equals(result))
			result = "CHAR"; // a dumb but plausible default.
		return result;
	}

	/**
	 * Load the mapping file WEBSPHERE_MAPPINGS_FILE_NAME which contain relations between DB field type and JDBC field type...
	 * Is it still necessary ?
	 * 
	 * @throws XDocletException
	 */
	private void loadMappings() throws XDocletException {
		if (mappings == null) {
			mappings = new Properties();
			try {
				InputStream s = getClass().getClassLoader().getResourceAsStream(WEBSPHERE_MAPPINGS_FILE_NAME);
				if (s != null) {
					mappings.load(s);
					System.out.println("Loaded mappings:");dumpMap(mappings);
				} else
					System.err.println("Unable to find '" + WEBSPHERE_MAPPINGS_FILE_NAME + "'");
			} catch (Throwable e) {
				System.err.println("Error loading '" + WEBSPHERE_MAPPINGS_FILE_NAME + "' (" + e + ")");
				throw new XDocletException(e.getMessage());
			} 
		}
	}

	private static Properties mappings = null;
	
	/**
	 * 
	 * TODO
	 * 
	 * @param dbSpecification
	 * @return
	 */
	private String getGeneralDBName(String dbSpecification) {
		if (dbSpecification.startsWith("INFORMIX"))
			return "INFORMIX";
		else if (dbSpecification.startsWith("DB2"))
			return "DB2";
		else if (dbSpecification.startsWith("ORACLE"))
			return "ORACLE"; 
		else
			return "UNKNOWN";
	}
	
	/**
	 * 
	 * TODO
	 * 
	 * 
	 * @param sqlType
	 * @return
	 */
	private String getGeneralJDBCType(String sqlType) {
		String result = "CHAR";
		if (sqlType.startsWith("CHAR("))
			result = "CHAR(";
		else
			result = sqlType;
		System.out.println(getClass().getName() + ".getGeneralJDBCType(" + sqlType + ") returns '" + result + "'");
		return result;
	}
	
	/**
	 * 
	 * TODO
	 * 
	 * @param p
	 * @return
	 */
	public String getShortDBName(Properties p) {
		String dbName = (String)p.get("dbType");
		if (dbName.startsWith("INFORMIX_V73"))
			return "INFORMIX_V73";
		else if (dbName.startsWith("ORACLE")) 
			return "ORACLE_V8";
		else
			return "UNKNOWN";
	}
	
	/**
	 * 
	 * TODO
	 * 
	 * @param content
	 * @param ignored
	 * @return
	 * @throws XDocletException
	 */
	public boolean ifCharacterType(String content, Properties ignored) throws XDocletException {
		//System.out.print("ifCharacterType();dumpMap(ignored);
		
		boolean result = false;
		
		String sqlType = getCurrentMethod().getDoc().getTagAttributeValue("ejb:persistence","jdbc-type");
		//System.out.println(sqlType+")");
		if (sqlType != null) {
			if (sqlType.startsWith("CHAR"))
				generate(content);
		}
		return result;
	}
	public String getColumnLength() {
		String length = "";
		String sqlType = getCurrentMethod().getDoc().getTagAttributeValue("ejb:persistence","sql-type");
		if (sqlType != null) {
			int startParen = sqlType.indexOf('(');
			if (startParen > -1) {
				int endParen = sqlType.indexOf(')');
				if (endParen > -1) {
					length = sqlType.substring(startParen+1,endParen);
				}
			}
		}
		return length;
	}
	
	
	/**
	 * Get the relationTable name of the table entering in relation with ???
	 * Look at the tag @websphere:relation table-name="...." 
	 * 
	 * @return relationTable name of the table entering in relation with ???
	 */
	public String getRelatedTable() {
		String relationTable = getCurrentMethod().getDoc().getTagAttributeValue("websphere:relation","table-name");
		return relationTable;
	}
	
	/**
	 * 
	 * get the name of the columns included in the relationship for primary key ? for foreign key ?
	 * Look at all the tag @websphere:relation-column column-name="....." and create the String RDBColumn_<name of the first column>RDBColumn_<name of the second column>RDBColumn....
	 * 
	 * @return name of the columns included in the relationship for primary key ? for foreign key ?
	 * 
	 * @author Denis Boutin (denisboutin@free.fr)
	 */
	public String getRelationshipKeyColumns() {
		String result = "";
		
		List columnList = getCurrentMethod().getDoc().getTags("websphere:relation-column");
		boolean first = true;
		for (Iterator columnIterator = columnList.iterator(); columnIterator.hasNext();) {
			XTag tag = (XTag)columnIterator.next();
			String columnName = tag.getAttributeValue("column-name");
			if (!first) {
				result = result + " RDBColumn_" +  columnName;
			} else {
				result = "RDBColumn_" + columnName;
				first = false;
			}
		}
		System.out.println(getClass().getName() + ".getRelationshipKeyColumns() returns '" + result + "'");
		
		return result;
	}
	
	private String _currentTableName = null;
	private Hashtable tables = null;

	/**
	 * Loop for generating block for all the remote tables defined in the tags :
	 * @websphere:relation-column remote-table="...."
	 *                            column-name="...."
	 * 
	 * @param content
	 * @throws XDocletException
	 * 
	 * @author Denis Boutin (denisboutin@free.fr)
	 */
	public void forAllForeignTables(String content) throws XDocletException{
		XMethod method = getCurrentMethod();
		List columnList = method.getDoc().getTags("websphere:relation-column");
		tables = new Hashtable();
		
		for (Iterator columns = columnList.iterator(); columns.hasNext();) {
			XTag tag = (XTag)columns.next();
			String tableName = tag.getAttributeValue("remote-table");
			if (tables.get(tableName) == null)
				tables.put(tableName,"RDBColumn_" + tag.getAttributeValue("remote-column-name"));
			else {
				String columnNames = (String)tables.get(tableName);
				columnNames += " RDBColumn_" + tag.getAttributeValue("remote-column-name");
				tables.put(tableName,columnNames);
			}
		}
		
		// loop through all tables, generating content
		for (Iterator tableLooper = tables.keySet().iterator(); tableLooper.hasNext();) {
			String tableName = (String)tableLooper.next();
			//String columnNames = (String)tables.get(tableName);
			_currentTableName = tableName;
			generate(content);
			
		}
	} // forAllForeignTables
	
	/**
	 * get the current table name beeing used
	 * 
	 * @return _currentTableName used
	 */
	public String tableName() {
		System.out.println("tableName() returns '" + _currentTableName + "'");
		return _currentTableName;
	}
	
	/**
	 * get the current table foreign key columns
	 * 
	 * @return the current table foreign key columns
	 */
	public String getCurrentTableForeignKeyColumns() {
		System.out.println("getCurrentTableForeignKeyColumns() returns '" + (String)tables.get(_currentTableName) + "'");
		return (String)tables.get(_currentTableName);
	}
	
	/**
	 * 
	 * TODO
	 * 
	 * 
	 * @return
	 */
	public String getConversionHRef() {
		String result = "Integer-INTEGER"; // just to have a default
		XMethod theMethod = getCurrentMethod();
		String jdbcType = theMethod.getDoc().getTagAttributeValue("ejb.persistence","jdbc-type");
		String sqlType = theMethod.getDoc().getTagAttributeValue("ejb:persistence","sql-type");
		if (jdbcType != null && sqlType != null) {
			if ("INTEGER".equalsIgnoreCase(jdbcType)) jdbcType = "Integer";
			else if ("CHAR".equalsIgnoreCase(jdbcType)) jdbcType = "String";
			//else jdbcType = "Unknown";
			
			if ("INTEGER".equalsIgnoreCase(sqlType)) sqlType = "INTEGER";
			else if (sqlType.startsWith("CHAR")) sqlType = "VARCHAR";
			//else sqlType = "UNKNOWN";
			result = jdbcType + "-" + sqlType;
		} else {
			System.out.println(getClass().getName() + ".getConversionHRef, a tag was null for method " +getCurrentClass().getName() + "." + theMethod.getName());
		}
		//System.out.println("getConversionHRef() for " + theMethod.getNameWithoutPrefix() + " returning " + result);
		//System.out.println("\tjdbcType=" + jdbcType + ", sqlType=" + sqlType);		
		return result;
	}
	
	/**
	 * TODO
	 * 
	 * 
	 * @param content
	 * @throws XDocletException
	 */
	public void ifMyRelationship(String content) throws XDocletException {
		System.out.println("WasEjbTagsHandler.ifMyRelationship()");
		System.out.println("\tLeft Class: " +  currentRelation.getLeft().getContainingClass());
		if (currentRelation.getLeft() == _currentClass)
			generate(content);
	}
	
	/**
	 * get the table name for the current relation ship
	 * Look at @websphere:relation table-name="...."
	 * 
	 * @return the table name for the current relationship
	 * @throws XDocletException
	 */
	public String getTableNameForRelationship() throws XDocletException {
		String tableName = getCurrentMethod().getDoc().getTagAttributeValue("websphere:relation","table-name");
		
		System.out.println("WasEjbTagsHandler.getTableNameForRelationship(" + currentRelation.getName() + ") returning '" + tableName + "'");
		System.out.println("\tClass: " + getCurrentClass() + ", method: " + getCurrentMethod());
		
		return tableName;
	}
	/*
	public String getTableNameForRelationship(Properties p) throws XDocletException {
		//System.out.println("WasEjbTagsHandler.getTableNameForRelationship(p," + currentRelation.getName() + ") returning '" + currentRelation.getTableName() + "'");
		return currentRelation.getTableName();
	}
	*/
	
	/**
	 * TODO 
	 * 
	 * @return TODO
	 */
	public String getForeignColumn() throws XDocletException {
		String result = "TODO";
		String maybe = currentRelation.getRightMethod().getDoc().getTagAttributeValue("websphere.relation-column","remote-table");
		//System.out.println(getCurrentClass().getName() + " " + currentRelation.getName() + " returns '" + maybe + "'");
		return result;
	}
	
	/**
	 * Print out a dump of the propertie file mapping the SQL types and JDBC types
	 * 
	 * @param p
	 */
	public void dumpMap(Map p) {
		for (Iterator i=p.keySet().iterator(); i.hasNext(); ) {
			String propName = (String)i.next();
			String value = (String)p.get(propName);
			System.out.println("\t" + propName + "=" + value);
		}
	} // dumpMap

	/**
	 * Print out all of the methods of the current class with their tag name and value 
	 */
	public void dumpMethod() {
		XClass clazz = getCurrentClass();
		XMethod method = getCurrentMethod();
		XDoc doc = method.getDoc();
		List tags = doc.getTags();
		for (Iterator taglooper = tags.iterator(); taglooper.hasNext();) {
			XTag tag = (XTag)taglooper.next();
			String tagName = tag.getName();
			String value = tag.getValue();
			System.out.println(clazz.getName() + "." + method.getName()+ ":\n\t" + tagName + "=" + value);
		}
	}
	XClass _currentClass = null;
	
	/**
	 * generate the block for all of the relationships defined in @ejb:relation
	 * 
	 * @see xdoclet.modules.ejb.dd.RelationTagsHandler#forAllRelationships(java.lang.String)
	 */
	public void forAllRelationships(String template) throws XDocletException {
		System.out.println("WasEjbTagsHandler.forAllRelationships()");
		_currentClass = getCurrentClass();
		
		System.out.println("**************************");
		System.out.println("**************************");
		System.out.println("**************************");
		System.out.println("********** for all relationships ****************");
		System.out.println("_currentClass.getCurrentClass() [qualified name] : " + _currentClass.getQualifiedName() );	
		System.out.println("**************************");
		System.out.println("**************************");
		System.out.println("**************************");
		super.forAllRelationships(template);
	}
	
	public String getRelationHolder() {
		return super.currentRelation.toString();
	
	}
	/**
	 * TODO
	 * 
	 * This method is never called
	 * 
	 * @param template
	 * @param log
	 * @throws XDocletException
	 */
	protected void generateRelationshipTemplates(String template, Log log)
		throws XDocletException {

		System.out.println("WasEjbTagsHandler.generateRelationshipTemplates(), getCurrentClass(): " + _currentClass);
		
		// Loop over all relations
		Iterator relationNameIterator = relationMap.keySet().iterator();
		
		while (relationNameIterator.hasNext()) {
			// The name is only needed to provide potential error messages.
			String relationName = (String) relationNameIterator.next();
		
			RelationHolder relationHolder = (RelationHolder) relationMap.get(relationName);
			if (relationHolder.getLeft() == _currentClass || relationHolder.getRight() == _currentClass) {		
				setCurrentClass(relationHolder.getLeft());
				setCurrentMethod(relationHolder.getLeftMethod());
				_currentTableName = relationHolder.getLeftMethod().getDoc().getTagAttributeValue("websphere:relation-column","remote-table");;
			
				currentRelation = relationHolder;
			
				if (log.isDebugEnabled()) {
					log.debug("Generating template for Relation: " + currentRelation);
				}
				generate(template);
			}
		}
	}
}
