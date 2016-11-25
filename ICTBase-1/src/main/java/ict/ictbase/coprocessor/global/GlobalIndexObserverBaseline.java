package ict.ictbase.coprocessor.global;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Durability;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class GlobalIndexObserverBaseline extends GlobalIndexBasicObserver {
	@Override
	public void prePut(final ObserverContext<RegionCoprocessorEnvironment> e,
			final Put put, final WALEdit edit, final Durability durability)
			throws IOException {
		super.prePut(e, put, edit, durability);
		long now = queueUtil.getNowTime();
		byte[] byteNow = Bytes.toBytes(now);
		Map<byte[], List<Cell>> familyMap = put.getFamilyCellMap();
		for (Entry<byte[], List<Cell>> entry : familyMap.entrySet()) {
			List<Cell> cells = entry.getValue();
			for (Cell cell : cells) {
				CellUtil.updateLatestStamp(cell, byteNow, 0);
			}
		}
		put.setAttribute("put_time_version", Bytes.toBytes(now));
	}
	
	public void postPut(final ObserverContext<RegionCoprocessorEnvironment> e,
			final Put put, final WALEdit edit, final Durability durability)
			throws IOException {
		synchronized(this){
			dataTableWithIndexes.readBaseAndDeleteOld(put);
			dataTableWithIndexes.insertNewToIndexes(put);
		}
	}
}
