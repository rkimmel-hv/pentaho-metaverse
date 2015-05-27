/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2014 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */

package com.pentaho.metaverse.analyzer.kettle.step.tableoutput;

import com.pentaho.dictionary.DictionaryConst;
import com.pentaho.dictionary.MetaverseTransientNode;
import com.pentaho.metaverse.api.IComponentDescriptor;
import com.pentaho.metaverse.api.IConnectionAnalyzer;
import com.pentaho.metaverse.api.IMetaverseBuilder;
import com.pentaho.metaverse.api.IMetaverseNode;
import com.pentaho.metaverse.api.IMetaverseObjectFactory;
import com.pentaho.metaverse.api.INamespace;
import com.pentaho.metaverse.api.MetaverseComponentDescriptor;
import com.pentaho.metaverse.api.analyzer.kettle.ComponentDerivationRecord;
import com.pentaho.metaverse.api.analyzer.kettle.step.ExternalResourceStepAnalyzer;
import com.pentaho.metaverse.api.analyzer.kettle.step.StepNodes;
import com.pentaho.metaverse.api.model.BaseDatabaseResourceInfo;
import com.pentaho.metaverse.testutils.MetaverseTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.tableoutput.TableOutputMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

/**
 * @author mburgess
 */
@RunWith(MockitoJUnitRunner.class)
public class TableOutputStepAnalyzerTest {

  private TableOutputStepAnalyzer analyzer;

  @Mock IMetaverseNode node;

  @Mock
  private IMetaverseBuilder mockBuilder;
  @Mock
  private TableOutputMeta meta;
  @Mock
  private INamespace mockNamespace;
  @Mock
  private StepMeta parentStepMeta;
  @Mock
  private TransMeta parentTransMeta;

  IComponentDescriptor descriptor;

  @Before
  public void setUp() throws Exception {
    IMetaverseObjectFactory factory = MetaverseTestUtils.getMetaverseObjectFactory();
    when( mockBuilder.getMetaverseObjectFactory() ).thenReturn( factory );
    analyzer = spy( new TableOutputStepAnalyzer() );
    analyzer.setConnectionAnalyzer( mock( IConnectionAnalyzer.class ) );
    analyzer.setMetaverseBuilder( mockBuilder );
    analyzer.setBaseStepMeta( meta );
    analyzer.setRootNode( node );
    analyzer.setParentTransMeta( parentTransMeta );
    analyzer.setParentStepMeta( parentStepMeta );

    when( mockNamespace.getParentNamespace() ).thenReturn( mockNamespace );
    descriptor = new MetaverseComponentDescriptor( "test", DictionaryConst.NODE_TYPE_TRANS_STEP, mockNamespace );
    analyzer.setDescriptor( descriptor );

    when( meta.getParentStepMeta() ).thenReturn( parentStepMeta );
    when( parentStepMeta.getParentTransMeta() ).thenReturn( parentTransMeta );
    when( parentStepMeta.getName() ).thenReturn( "test" );
    when( parentStepMeta.getStepID() ).thenReturn( "TableOutputStep" );
  }

  @Test
  public void testGetSupportedSteps() {
    TableOutputStepAnalyzer analyzer = new TableOutputStepAnalyzer();
    Set<Class<? extends BaseStepMeta>> types = analyzer.getSupportedSteps();
    assertNotNull( types );
    assertEquals( types.size(), 1 );
    assertTrue( types.contains( TableOutputMeta.class ) );
  }

  @Test
  public void testCustomAnalyze() throws Exception {
    when( meta.truncateTable() ).thenReturn( true );
    IMetaverseNode node = new MetaverseTransientNode( "new node" );
    analyzer.customAnalyze( meta, node );
    assertNotNull( node );

    assertTrue( (Boolean) node.getProperty( TableOutputStepAnalyzer.TRUNCATE_TABLE ) );
  }

  @Test
  public void testCreateTableNode() throws Exception {
    IConnectionAnalyzer connectionAnalyzer = mock( IConnectionAnalyzer.class );

    doReturn( connectionAnalyzer ).when( analyzer ).getConnectionAnalyzer();
    IMetaverseNode connNode = mock( IMetaverseNode.class );
    when( connectionAnalyzer.analyze( any( IComponentDescriptor.class ), anyObject() ) ).thenReturn( connNode );


    BaseDatabaseResourceInfo resourceInfo = mock( BaseDatabaseResourceInfo.class );
    Map<Object, Object> attributes = new HashMap<>();
    attributes.put( DictionaryConst.PROPERTY_TABLE, "tableName" );
    attributes.put( DictionaryConst.PROPERTY_SCHEMA, "schemaName" );
    when( resourceInfo.getAttributes() ).thenReturn( attributes );

    IMetaverseNode connectionNode = mock( IMetaverseNode.class );
    doReturn( connectionNode ).when( analyzer ).getConnectionNode();
    when( connectionNode.getLogicalId() ).thenReturn( "CONNECTION_ID" );

    IMetaverseNode resourceNode = analyzer.createTableNode( resourceInfo );
    assertEquals( "tableName", resourceNode.getProperty( DictionaryConst.PROPERTY_TABLE ) );
    assertEquals( "tableName", resourceNode.getName() );
    assertEquals( "schemaName", resourceNode.getProperty( DictionaryConst.PROPERTY_SCHEMA ) );
    assertEquals( "CONNECTION_ID", resourceNode.getProperty( DictionaryConst.PROPERTY_NAMESPACE ) );
  }

  @Test
  public void testCreateTableNode_nullSchema() throws Exception {
    IConnectionAnalyzer connectionAnalyzer = mock( IConnectionAnalyzer.class );

    doReturn( connectionAnalyzer ).when( analyzer ).getConnectionAnalyzer();
    IMetaverseNode connNode = mock( IMetaverseNode.class );
    when( connectionAnalyzer.analyze( any( IComponentDescriptor.class ), anyObject() ) ).thenReturn( connNode );


    BaseDatabaseResourceInfo resourceInfo = mock( BaseDatabaseResourceInfo.class );
    Map<Object, Object> attributes = new HashMap<>();
    attributes.put( DictionaryConst.PROPERTY_TABLE, "tableName" );
    when( resourceInfo.getAttributes() ).thenReturn( attributes );

    IMetaverseNode connectionNode = mock( IMetaverseNode.class );
    doReturn( connectionNode ).when( analyzer ).getConnectionNode();
    when( connectionNode.getLogicalId() ).thenReturn( "CONNECTION_ID" );

    IMetaverseNode resourceNode = analyzer.createTableNode( resourceInfo );
    assertEquals( "tableName", resourceNode.getProperty( DictionaryConst.PROPERTY_TABLE ) );
    assertEquals( "tableName", resourceNode.getName() );
    assertNull( resourceNode.getProperty( DictionaryConst.PROPERTY_SCHEMA ) );
    assertEquals( "CONNECTION_ID", resourceNode.getProperty( DictionaryConst.PROPERTY_NAMESPACE ) );
  }

  @Test
  public void testCreateTableNode_nullTable() throws Exception {
    IConnectionAnalyzer connectionAnalyzer = mock( IConnectionAnalyzer.class );

    doReturn( connectionAnalyzer ).when( analyzer ).getConnectionAnalyzer();
    IMetaverseNode connNode = mock( IMetaverseNode.class );
    when( connectionAnalyzer.analyze( any( IComponentDescriptor.class ), anyObject() ) ).thenReturn( connNode );


    BaseDatabaseResourceInfo resourceInfo = mock( BaseDatabaseResourceInfo.class );
    Map<Object, Object> attributes = new HashMap<>();
    when( resourceInfo.getAttributes() ).thenReturn( attributes );

    IMetaverseNode connectionNode = mock( IMetaverseNode.class );
    doReturn( connectionNode ).when( analyzer ).getConnectionNode();
    when( connectionNode.getLogicalId() ).thenReturn( "CONNECTION_ID" );

    IMetaverseNode resourceNode = analyzer.createTableNode( resourceInfo );
    assertNull( resourceNode.getProperty( DictionaryConst.PROPERTY_TABLE ) );
    assertNull( resourceNode.getProperty( DictionaryConst.PROPERTY_SCHEMA ) );
    assertEquals( "CONNECTION_ID", resourceNode.getProperty( DictionaryConst.PROPERTY_NAMESPACE ) );
  }

  @Test
  public void testGetResourceInputNodeType() throws Exception {
    assertNull( analyzer.getResourceInputNodeType() );
  }

  @Test
  public void testGetResourceOutputNodeType() throws Exception {
    assertEquals( DictionaryConst.NODE_TYPE_DATA_COLUMN, analyzer.getResourceOutputNodeType() );
  }

  @Test
  public void testIsOutput() throws Exception {
    assertTrue( analyzer.isOutput() );
  }

  @Test
  public void testIsInput() throws Exception {
    assertFalse( analyzer.isInput() );
  }

  @Test
  public void testGetUsedFields() throws Exception {
    assertNull( analyzer.getUsedFields( meta ) );
  }

  @Test
  public void testGetOutputResourceFields() throws Exception {
    String[] outputFields = new String[2];
    outputFields[0] = "field1";
    outputFields[1] = "field2";

    when( meta.getFieldDatabase() ).thenReturn( outputFields );

    Set<String> outputResourceFields = analyzer.getOutputResourceFields( meta );

    assertEquals( outputFields.length, outputResourceFields.size() );
    for ( String outputField : outputFields ) {
      assertTrue( outputResourceFields.contains( outputField ) );
    }
  }

  @Test
  public void testGetChangeRecords() throws Exception {
    String[] tableFields = new String[] { "field1", "field2" };
    when( meta.getFieldDatabase() ).thenReturn( tableFields );

    String[] streamFields = new String[] { "f1", "field2", "field3" };
    when( meta.getFieldStream() ).thenReturn( streamFields );

    StepNodes inputs = new StepNodes();
    inputs.addNode( "prevStep", "f1", node );
    inputs.addNode( "prevStep", "field2", node );
    inputs.addNode( "prevStep", "field3", node );
    inputs.addNode( ExternalResourceStepAnalyzer.RESOURCE, "field1", node );
    inputs.addNode( ExternalResourceStepAnalyzer.RESOURCE, "field2", node );

    doReturn( inputs ).when( analyzer ).getInputs();

    Set<ComponentDerivationRecord> changeRecords = analyzer.getChangeRecords( meta );
    assertNotNull( changeRecords );
    assertEquals( tableFields.length, changeRecords.size() );

    for ( ComponentDerivationRecord changeRecord : changeRecords ) {
      if ( "f1".equals( changeRecord.getOriginalEntityName() ) ) {
        assertEquals( "field1", changeRecord.getChangedEntityName() );
      } else if ( "field2".equals( changeRecord.getOriginalEntityName() ) ) {
        assertEquals( "field2", changeRecord.getChangedEntityName() );
      } else {
        fail( "We encountered a change record that shouldn't be here - " + changeRecord.toString() );
      }
    }

  }

  @Test
  public void testGetOutputRowMetaInterfaces() throws Exception {
    String[] nextStepNames = new String[] { "nextStep1" };
    when( parentTransMeta.getNextStepNames( parentStepMeta ) ).thenReturn( nextStepNames );

    RowMetaInterface rmi = mock( RowMetaInterface.class );
    doReturn( rmi ).when( analyzer ).getOutputFields( meta );

    String[] tableFields = new String[] { "field1", "field2" };
    when( meta.getFieldDatabase() ).thenReturn( tableFields );

    Map<String, RowMetaInterface> outputs = analyzer.getOutputRowMetaInterfaces( meta );
    assertNotNull( outputs );

    assertEquals( 2, outputs.size() );
    RowMetaInterface resourceRow = outputs.get( ExternalResourceStepAnalyzer.RESOURCE );
    assertNotNull( resourceRow );

    for ( String tableField : tableFields ) {
      assertNotNull( resourceRow.searchValueMeta( tableField ) );
    }
    assertEquals( rmi, outputs.get( "nextStep1" ) );
  }
}
