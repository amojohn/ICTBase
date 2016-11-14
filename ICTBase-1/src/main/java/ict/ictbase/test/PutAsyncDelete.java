package ict.ictbase.test;

import ict.ictbase.client.HTableGetByIndex;
import ict.ictbase.util.HIndexConstantsAndUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

public class PutAsyncDelete {
	private static String testTableName = "test_async_delete_1";
	private static String columnFamily = "cf";
	private static String indexedColumnName = "country";
	private static Configuration conf;
	private static String coprocessorJarLoc = "hdfs://data8:9000/jar/ICTBase-1-0.0.1-SNAPSHOT.jar";
	private static HTableGetByIndex htable;

	public static void initTables(Configuration conf, String testTableName,
			String columnFamily, String indexedColumnName) throws Exception {
		Connection con = ConnectionFactory.createConnection(conf);
		Admin admin = con.getAdmin();
		TableName tn = TableName.valueOf(testTableName);

		if (admin.isTableAvailable(tn)) {
			HIndexConstantsAndUtils.deleteTable(conf,
					Bytes.toBytes(testTableName));
		}

		
		HIndexConstantsAndUtils.createAndConfigBaseTable(conf,
				Bytes.toBytes(testTableName), Bytes.toBytes(columnFamily),
				new String[] { indexedColumnName });

		byte[] indexTableName = HIndexConstantsAndUtils.generateIndexTableName(
				Bytes.toBytes(testTableName), Bytes.toBytes(columnFamily),
				Bytes.toBytes(indexedColumnName));

		TableName indexTN = TableName.valueOf(indexTableName);

		if (admin.isTableAvailable(indexTN)) {
			HIndexConstantsAndUtils.deleteTable(conf, indexTableName);
		}

		HIndexConstantsAndUtils.createAndConfigIndexTable(conf, indexTableName,
				Bytes.toBytes(columnFamily));
	}

	public static void initCoProcessors(Configuration conf,
			String coprocessorJarLoc, HTableGetByIndex htable) throws Exception {
		int coprocessorIndex = 1;
		HIndexConstantsAndUtils.updateCoprocessor(conf, htable.getTableName(),
				coprocessorIndex++, true, coprocessorJarLoc,
				"ict.ictbase.coprocessor.IndexObserverAsyncMaintain");

//		htable.configPolicy(HTableGetByIndex.PLY_READCHECK);
	}

	public static void loadData() throws IOException {
		// load data
		String rowkeyStr = "key_async";
		byte[] rowKey = Bytes.toBytes(rowkeyStr);
		for (int i = 0; i < 5; i++) {
			Put p = new Put(rowKey);
			long ts = 100+i;
			p.addColumn(Bytes.toBytes(columnFamily),
					Bytes.toBytes(indexedColumnName), ts,
					Bytes.toBytes("v" + i));
			p.setAttribute("put_time_version", Bytes.toBytes(ts));
			Map<String,byte[]> map = p.getAttributesMap();
			
			for(Entry<String,byte[]> entry : map.entrySet()){
				System.out.println(entry.getKey()+"\t "+Bytes.toLong(entry.getValue()));
			}
			
			htable.put(p);
		}

	}

	public static void main(String[] args) throws Exception {
		conf = HBaseConfiguration.create();
		if (args.length == 3) {
			testTableName = args[0];
			columnFamily = args[1];
			indexedColumnName = args[2];

		}
		initTables(conf, testTableName, columnFamily, indexedColumnName);
		htable = new HTableGetByIndex(conf, Bytes.toBytes(testTableName));
		initCoProcessors(conf, coprocessorJarLoc, htable);

		loadData();

		// getByIndex
		htable.configPolicy(HTableGetByIndex.PLY_FASTREAD);
		List<byte[]> res = htable.getByIndex(Bytes.toBytes(columnFamily),
				Bytes.toBytes(indexedColumnName), Bytes.toBytes("v4"));
		assert (res != null && res.size() != 0);
		System.out.println("Result is " + Bytes.toString(res.get(0)));
	}
}
