/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.kafka.nativeimpl.producer.action;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.KafkaException;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.kafka.util.KafkaUtils;
import org.ballerinalang.model.NativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.util.exceptions.BallerinaException;

import java.util.Objects;
import java.util.Properties;

import static org.ballerinalang.kafka.util.KafkaConstants.KAFKA_NATIVE_PACKAGE;
import static org.ballerinalang.kafka.util.KafkaConstants.NATIVE_PRODUCER;
import static org.ballerinalang.kafka.util.KafkaConstants.NATIVE_PRODUCER_CONFIG;
import static org.ballerinalang.kafka.util.KafkaConstants.ORG_NAME;
import static org.ballerinalang.kafka.util.KafkaConstants.PACKAGE_NAME;
import static org.ballerinalang.kafka.util.KafkaConstants.PRODUCER_CONFIG_STRUCT_NAME;
import static org.ballerinalang.kafka.util.KafkaConstants.PRODUCER_STRUCT_NAME;

/**
 * Native action initializes a producer instance for connector.
 */
@BallerinaFunction(
        orgName = ORG_NAME,
        packageName = PACKAGE_NAME,
        functionName = "init",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = PRODUCER_STRUCT_NAME,
                structPackage = KAFKA_NATIVE_PACKAGE),
        args = {
                @Argument(name = "producerConfig", type = TypeKind.STRUCT, structType = PRODUCER_CONFIG_STRUCT_NAME)
        }
       )
public class Init implements NativeCallableUnit {

    @Override
    public void execute(Context context, CallableUnitCallback callableUnitCallback) {
        BStruct producerConnector = (BStruct) context.getRefArgument(0);

        BStruct producerConf = (BStruct) context.getRefArgument(1);
        Properties producerProperties = KafkaUtils.processKafkaProducerConfig(producerConf);

        try {
            KafkaProducer<byte[], byte[]> kafkaProducer = new KafkaProducer<>(producerProperties);

            if (Objects.isNull(kafkaProducer)) {
                throw new BallerinaException("Kafka producer has not been initialized properly.");
            }

            if (producerProperties.get(ProducerConfig.TRANSACTIONAL_ID_CONFIG) != null) {
                kafkaProducer.initTransactions();
            }

            BMap producerMap = (BMap) producerConnector.getRefField(0);
            BStruct producerStruct = KafkaUtils.createKafkaPackageStruct(context, PRODUCER_STRUCT_NAME);
            producerStruct.addNativeData(NATIVE_PRODUCER, kafkaProducer);
            producerStruct.addNativeData(NATIVE_PRODUCER_CONFIG, producerProperties);

            producerMap.put(new BString(NATIVE_PRODUCER), producerStruct);
        } catch (IllegalStateException | KafkaException e) {
            throw new BallerinaException("Failed to initialize the producer " + e.getMessage(), e, context);
        }
        callableUnitCallback.notifySuccess();
    }

    @Override
    public boolean isBlocking() {
        return false;
    }
}
