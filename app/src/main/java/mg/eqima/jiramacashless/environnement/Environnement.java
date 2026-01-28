package mg.eqima.jiramacashless.environnement;

import android.util.Log;

public class Environnement {
    //1 preprod 2 prod
    private int environnement = 1 ;

    //

    // URL backoffice Jirakaiky:
    private final String domain_name_prod = "https://cashless.eqima.org";
    private final String domain_name_preprod = "https://preprod.api.cashless.eqima.org";

    // URL MVOLA:
    private static final String dev_api_mvola = "https://devapi.mvola.mg";
    private static final String api_mvola = "https://api.mvola.mg";
    private final String merchant_number_1 = "0343500004";
    private final String merchant_number_2 = "0342135891";

    private final String consumer_key_1 = "cFh7qfpYucBLQUqcUjj38Z3LZ0Ia";
    private final String consumer_secret_1 = "ASXbXl4BPmtQlS3p2qS1s5nJgSYa";

    private static final String consumer_key_2 = "34wcX2Jh4wvClmhPiuQAx0TRJoYa";
    private static final String consumer_secret_2 = "8z_RkyRumYaUxU5fRiQMSSXsbmIa";


    public Environnement()
    {

    }

    public String getMerchantNumber()
    {
        String merchantNumber = merchant_number_1;
        if(this.environnement==2)
        {
            merchantNumber = merchant_number_2;
        }

        return merchantNumber;
    }

    public String getDomainNameMvola()
    {
        String domainName = dev_api_mvola;
        if(this.environnement==2)
        {
            domainName = api_mvola;
        }

        return domainName;
    }

    public String getDomainName()
    {
        String domainName = domain_name_preprod;
        if(this.environnement==2)
        {
            domainName = domain_name_prod;
        }

        return domainName;
    }

    public String getAEncoder()
    {
        String aEncoder = consumer_key_1+":"+consumer_secret_1;
        if(this.environnement==2)
        {
            aEncoder = consumer_key_2+":"+consumer_secret_2;
        }

        Log.e("MVOLA", "A encoder: "+ aEncoder);
        return aEncoder;
    }
}
