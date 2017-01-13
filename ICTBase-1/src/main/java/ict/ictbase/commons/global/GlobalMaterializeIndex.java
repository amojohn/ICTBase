package ict.ictbase.commons.global;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.client.HTable;


public interface GlobalMaterializeIndex {
	
	//global index
    public List<String> getByIndexByRange(HTable indexTable, byte[] valueStart, byte[] valueStop) throws IOException;
    public void putToIndex(HTable indexTable, byte[] dataValue, byte[] dataKey) throws IOException;
    public boolean deleteFromIndex(HTable indexTable, byte[] dataValue, byte[] dataKey) throws IOException;
}
