package ict.ictbase.coprocessor.global;

import ict.ictbase.commons.global.GlobalHTableUpdateIndexByPut;
import ict.ictbase.coprocessor.LoggedObserver;
import ict.ictbase.util.global.GlobalQueueUtil;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.InternalScanner;
import org.apache.hadoop.hbase.regionserver.RegionScanner;
import org.apache.hadoop.hbase.regionserver.ScanType;
import org.apache.hadoop.hbase.regionserver.Store;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;

public class GlobalIndexBasicObserver extends LoggedObserver {
    private boolean initialized;

    
    protected GlobalHTableUpdateIndexByPut dataTableWithIndexes = null;
    protected GlobalQueueUtil queueUtil = null;
    
    private void tryInitialize(HTableDescriptor desc) throws IOException {
        if(initialized == false) {
            synchronized(this) {
                if(initialized == false) {
                    Configuration conf = HBaseConfiguration.create();
                    dataTableWithIndexes = new GlobalHTableUpdateIndexByPut(conf, desc.getTableName().getName()); //this will make copy of data table instance.
                    queueUtil = new GlobalQueueUtil(dataTableWithIndexes);
                    initialized = true;
                }
            }
        }
    }
    

    @Override
    public void start(CoprocessorEnvironment e) throws IOException {
        setFunctionLevelLogging(false);
        initialized = false;
        super.start(e);
    }

    @Override
    public void prePut(final ObserverContext<RegionCoprocessorEnvironment> e, final Put put, final WALEdit edit, final Durability durability) throws IOException {
        super.prePut(e, put, edit, durability);
        tryInitialize(e.getEnvironment().getRegion().getTableDesc());
    }

    @Override
    public void postPut(final ObserverContext<RegionCoprocessorEnvironment> e, final Put put, final WALEdit edit, final Durability durability) throws IOException {
        super.postPut(e, put, edit, durability);
    }

    @Override
    public void preDelete(ObserverContext<RegionCoprocessorEnvironment> e, Delete delete, WALEdit edit, final Durability durability) throws IOException {
        super.preDelete(e, delete, edit, durability);
        tryInitialize(e.getEnvironment().getRegion().getTableDesc());
    }

    @Override
    public InternalScanner preCompact(ObserverContext<RegionCoprocessorEnvironment> e,
      final Store store, final InternalScanner scanner, final ScanType scanType) throws IOException{
        InternalScanner toRet = super.preCompact(e, store, scanner, scanType);
        tryInitialize(e.getEnvironment().getRegion().getTableDesc());
        return toRet;
    }

    @Override
    public void preGetOp(ObserverContext<RegionCoprocessorEnvironment> e, Get get, List<Cell> result) throws IOException {
        super.preGetOp(e, get, result);
        tryInitialize(e.getEnvironment().getRegion().getTableDesc());
    }
    
    @Override
    public RegionScanner preScannerOpen(
			final ObserverContext<RegionCoprocessorEnvironment> e,
			final Scan scan, final RegionScanner s) throws IOException {
    	super.preScannerOpen(e, scan, s);
    	tryInitialize(e.getEnvironment().getRegion().getTableDesc());
    	return s;
    }
    
    @Override
    public void stop(CoprocessorEnvironment e) throws IOException { 
        super.stop(e);
        if(dataTableWithIndexes != null){
            dataTableWithIndexes.close();
        }
    }
}
