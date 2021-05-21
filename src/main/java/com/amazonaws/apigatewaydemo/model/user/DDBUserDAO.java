/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.apigatewaydemo.model.user;

import software.amazon.awssdk.regions.Region;
import com.amazonaws.apigatewaydemo.configuration.DynamoDBConfiguration;
import com.amazonaws.apigatewaydemo.exception.DAOException;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.HashMap;

/**
 * DynamoDB implementation of the UserDAO interface. This class reads the configuration from the DyanmoDBConfiguration
 * object in the com.amazonaws.apigatewaydemo.configuration package. Credentials to access DynamoDB are retrieved from
 * the Lambda environment.
 * <p/>
 * The table in DynamoDB should be created with an Hash Key called username.
 */
public class DDBUserDAO implements UserDAO {

    private static DDBUserDAO instance = null;
    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<User> table;

    /**
     * Returns an initialized instance of the DDBUserDAO object. DAO objects should be retrieved through the DAOFactory
     * class
     *
     * @return An initialized instance of the DDBUserDAO object
     */
    public static DDBUserDAO getInstance() {
        if (instance == null) {
            instance = new DDBUserDAO();
        }
        return instance;
    }

    protected DDBUserDAO() {

        Region region = Region.US_EAST_2;
        DynamoDbClient ddb = DynamoDbClient.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddb)
                .build();

        // Map Table Using Bean
        table = enhancedClient.table(DynamoDBConfiguration.USERS_TABLE_NAME, TableSchema.fromBean(User.class));
    }

    /**
     * Queries DynamoDB to find a user by its Username
     *
     * @param username The username to search for
     * @return A populated User object, null if the user was not found
     * @throws DAOException
     */
    public User getUserByName(String username) throws DAOException {
        if (username == null || username.trim().equals("")) {
            throw new DAOException("Cannot lookup null or empty user");
        }
        Key key = Key.builder()
                .partitionValue(username)
                .build();
        // Get Item
        return table.getItem(r -> r.key(key));
    }

    /**
     * Inserts a new row in the DynamoDB users table.
     *
     * @param user The new user information
     * @return The username that was just inserted in DynamoDB
     * @throws DAOException
     */
    public String createUser(User user) throws DAOException {
        if (user.getUsername() == null || user.getUsername().trim().equals("")) {
            throw new DAOException("Cannot create user with empty username");
        }
        if (getUserByName(user.getUsername()) != null) {
            throw new DAOException("Username must be unique");
        }
        // Add Item
        table.putItem(user);
        return user.getUsername();
    }
}
