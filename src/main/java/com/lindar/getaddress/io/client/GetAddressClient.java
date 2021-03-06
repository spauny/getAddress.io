package com.lindar.getaddress.io.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lindar.getaddress.io.client.util.GetAddressAPI;
import com.lindar.getaddress.io.client.vo.AddressVO;
import com.lindar.getaddress.io.client.vo.PostcodeVO;
import com.lindar.wellrested.WellRestedRequest;
import com.lindar.wellrested.vo.Result;
import com.lindar.wellrested.vo.ResultBuilder;
import com.lindar.wellrested.vo.WellRestedResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Slf4j
public class GetAddressClient {

    public static final String START = "?";
    public static final String AND = "&";
    public static final String API_KEY_QUERY = "api-key=%s";

    private final GetAddressAPI API;
    private final String apiKey;

    /**
     * Creates a getAddress.io client with the default API root path:
     * https://api.getAddress.io/v2/uk/ . Please provide your valid API key. If
     * you don't have one you can register for a free account on:
     * https://getaddress.io/
     *
     * @param apiKey your valid getAddress.io API key.
     */
    public GetAddressClient(String apiKey) {
        this.API = new GetAddressAPI();
        this.apiKey = apiKey;
    }

    /**
     * Creates a getAddress.io client with a custom getAddress.io API root path
     * - useful for when you host your own instance of getAddress.io or the
     * default API changes and the library hasn't been updated yet. Please
     * provide your valid API key. If you don't have one you can register for a
     * free account on: https://getaddress.io/
     *
     * @param getAddressAPIRootpath the custom getAddress.io API root path
     * @param apiKey your valid getAddress.io API key.
     * @throws IllegalArgumentException when the argument provided is blank -
     * please use the empty constructor if you want to use the default API path
     */
    public GetAddressClient(String getAddressAPIRootpath, String apiKey) {
        if (StringUtils.isBlank(getAddressAPIRootpath)) {
            throw new IllegalArgumentException("You provided a blank getAddress API root path. If you want to use the default getAddress.io API root path then use the default constructor");
        }
        this.API = new GetAddressAPI(getAddressAPIRootpath);
        this.apiKey = apiKey;
    }

    /**
     * Lookup a postcode. Returns all available data if found. Returns 404 if
     * postcode does not exist. Returns 401 if not authorized and make sure you don't go over your daily limit!
     *
     * @param postcode
     * @return
     */
    public Result<PostcodeVO> lookupPostcode(String postcode) {
        return processGetRequestAndReturnResponse(String.format(API.LOOKUP_POSTCODE + START + API_KEY_QUERY, postcode, this.apiKey));
    }

    /**
     * Lookup a postcode and house number. Returns all available data if found.
     * Returns 404 if postcode does not exist.
     *
     * @param postcode
     * @param houseNumber
     * @return
     */
    public Result<PostcodeVO> lookupPostcodeAndHouseNumber(String postcode, int houseNumber) {
        return processGetRequestAndReturnResponse(String.format(API.LOOKUP_POSTCODE_AND_HOUSE_NUMBER + START + API_KEY_QUERY, postcode, houseNumber, apiKey));
    }
    
    /**
     * Returns a list of postcodes - please NOTE: Bulk Requests are not allowed for the free plan or the second account plan. 
     * You need a 2000 requests or 10000 requests plan in order to make bulk requests otherwise you get a 401 Unauthorized response
     * @param postcodes
     * @return
     */
    public Result<List<PostcodeVO>> bulkLookupPostcodes(List<String> postcodes) {
        String url = String.format(API.BATCH_LOOKUP_POSTCODE 
                + START + API_KEY_QUERY, StringUtils.join(postcodes, ","), apiKey);

        WellRestedResponse serverResponse = WellRestedRequest.builder().url(url).build().get().submit();

        ResultBuilder<List<PostcodeVO>> response = ResultBuilder.empty();
        if (serverResponse.isValid()) {
            Gson gson = new Gson();
            List<PostcodeVO> postcodeVOs = gson.fromJson(serverResponse.getServerResponse(), new TypeToken<List<PostcodeVO>>() {
            }.getType());
            response.data(postcodeVOs).success(true);
        } else {
            response.success(false);
        }
        return response.build();
    }

    private Result<PostcodeVO> processGetRequestAndReturnResponse(String url) {
        WellRestedResponse serverResponse = WellRestedRequest.builder().url(url).build().get().submit();

        return getResponseFromServerResponse(serverResponse);
    }

    private Result<PostcodeVO> getResponseFromServerResponse(WellRestedResponse serverResponse) {
        ResultBuilder<PostcodeVO> response = ResultBuilder.empty();
        if (serverResponse.isValid()) {
            Gson gson = new Gson();
            PostcodeVO postcodeVO = gson.fromJson(serverResponse.getServerResponse(), new TypeToken<PostcodeVO>() {}.getType());
            for (String address : postcodeVO.getAddresses()) {
                String[] addressComponents = address.split(",");
                if (addressComponents.length != 7) {
                    log.warn("Address provided appear to be invalid. Please check with getAddress.io and try again");
                    continue;
                }
                AddressVO addressVO = new AddressVO();
                addressVO.setLine1(addressComponents[0]);
                addressVO.setLine2(addressComponents[1]);
                addressVO.setLine3(addressComponents[2]);
                addressVO.setLine4(addressComponents[3]);
                addressVO.setLocality(addressComponents[4]);
                addressVO.setCity(addressComponents[5]);
                addressVO.setCounty(addressComponents[6]);
                postcodeVO.getCompiledAddresses().add(addressVO);
            }
            response.data(postcodeVO).success(true).visible(false);
        } else {
            response.msg("Invalid or nonexistent postcode").success(false).visible(true);
        }
        return response.build();
    }
}
