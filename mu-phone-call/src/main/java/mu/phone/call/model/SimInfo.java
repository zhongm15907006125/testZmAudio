package mu.phone.call.model;

/**
 * SIM卡信息
 * @author LiYejun
 * @date 2023/7/2
 */
public class SimInfo {

    private String number;

    private String operatorName;

    private String iccId;

    public SimInfo() {
        super();
    }

    public SimInfo(String number, String operatorName, String iccId) {
        this();
        this.number = number;
        this.operatorName = operatorName;
        this.iccId = iccId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getIccId() {
        return iccId;
    }

    public void setIccId(String iccId) {
        this.iccId = iccId;
    }
}
