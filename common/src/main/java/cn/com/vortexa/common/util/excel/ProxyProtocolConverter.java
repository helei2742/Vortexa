package cn.com.vortexa.common.util.excel;

import cn.com.vortexa.common.constants.ProxyProtocol;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

public class ProxyProtocolConverter implements Converter<ProxyProtocol> {

    @Override
    public Class<?> supportJavaTypeKey() {
        return ProxyProtocol.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public ProxyProtocol convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        return ProxyProtocol.valueOf(cellData.getStringValue().toUpperCase());
    }
}
