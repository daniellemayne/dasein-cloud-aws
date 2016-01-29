/*
 *  *
 *  Copyright (C) 2009-2015 Dell, Inc.
 *  See annotations for authorship information
 *
 *  ====================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ====================================================================
 *
 */

package org.dasein.cloud.aws.compute;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AwsTestBase;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeCreateOptions;
import org.dasein.cloud.compute.VolumeFilterOptions;
import org.dasein.cloud.compute.VolumeFormat;
import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.compute.VolumeState;
import org.dasein.cloud.compute.VolumeType;
import org.dasein.cloud.dc.DataCenterServices;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.dasein.util.uom.storage.StorageUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Document;

import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Jeffrey Yan on 1/25/2016.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AWSCloud.class, EBSVolume.class })
public class EBSVolumeTest extends AwsTestBase {

    private EBSVolume ebsVolume;

    @Before
    public void setUp() {
        super.setUp();
        Mockito.mock(DataCenterServices.class);
        PowerMockito.doReturn(cloudMock).when(awsCloudStub).getDataCenterServices();


        ebsVolume = new EBSVolume(awsCloudStub);
    }

    protected Document resource(String resourceName) throws Exception {
        return super.resource("org/dasein/cloud/aws/compute/volume/" + resourceName);
    }

    @Test
    public void testListVolumeProducts() throws CloudException, InternalException {
        List<VolumeProduct> volumeProducts = (List<VolumeProduct>) ebsVolume.listVolumeProducts();
        assertThat("Product count should be greater than zero", volumeProducts.size(), greaterThan(0));
    }

    @Test
    public void testGetVolume() throws Exception {
        String volumeId = "vol-1a2b3c4d";

        EC2Method describeVolumesStub = mock(EC2Method.class);
        when(describeVolumesStub.invoke()).thenReturn(resource("describe_volumes.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("VolumeId.1", volumeId), hasEntry("Action", "DescribeVolumes"))))
                .thenReturn(describeVolumesStub);

        Volume volume = ebsVolume.getVolume(volumeId);

        assertEquals("standard", volume.getProviderProductId());
        assertEquals(VolumeType.HDD, volume.getType());
        assertEquals(VolumeFormat.BLOCK, volume.getFormat());

        assertEquals(volumeId, volume.getProviderVolumeId());
        assertEquals(volumeId, volume.getName());
        assertEquals(volumeId, volume.getDescription());
        assertEquals(80.0, volume.getSize().getQuantity());
        assertTrue(volume.getSize().getUnitOfMeasure() instanceof Gigabyte);
        assertNull(volume.getProviderSnapshotId());
        assertEquals("eu-west-1", volume.getProviderRegionId());
        assertEquals(1452148872485l, volume.getCreationTimestamp());
        assertEquals(VolumeState.AVAILABLE, volume.getCurrentState());

        assertEquals("i-1a2b3c4d", volume.getProviderVirtualMachineId());
        assertEquals("/dev/sdh", volume.getDeviceId());
    }

    @Test
    public void testListVolumes() throws Exception {
        EC2Method describeVolumesStub = mock(EC2Method.class);
        when(describeVolumesStub.invoke()).thenReturn(resource("describe_volumes.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(hasEntry("Action", "DescribeVolumes"))))
                .thenReturn(describeVolumesStub);

        List<Volume> volumes = (List<Volume>) ebsVolume
                .listVolumes(VolumeFilterOptions.getInstance().attachedTo("i-1a2b3c4d").matchingRegex("^vol-1a.*"));

        assertEquals(1, volumes.size());
    }

    @Test
    public void testListVolumesFilter() throws Exception {
        EC2Method describeVolumesStub = mock(EC2Method.class);
        when(describeVolumesStub.invoke()).thenReturn(resource("describe_volumes.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(hasEntry("Action", "DescribeVolumes"))))
                .thenReturn(describeVolumesStub);

        List<Volume> volumes = (List<Volume>) ebsVolume
                .listVolumes(VolumeFilterOptions.getInstance().attachedTo("i-2a3b4c5d"));

        assertEquals(0, volumes.size());
    }

    @Test
    public void testListVolumeStatus() throws Exception {
        EC2Method describeVolumesStub = mock(EC2Method.class);
        when(describeVolumesStub.invoke()).thenReturn(resource("describe_volumes.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(hasEntry("Action", "DescribeVolumes"))))
                .thenReturn(describeVolumesStub);

        List<ResourceStatus> resourceStatuses = (List<ResourceStatus>) ebsVolume.listVolumeStatus();
        assertEquals(1, resourceStatuses.size());
        assertEquals("vol-1a2b3c4d", resourceStatuses.get(0).getProviderResourceId());
        assertEquals(VolumeState.AVAILABLE, resourceStatuses.get(0).getResourceStatus());
    }

    @Test
    public void testCreateVolume() throws Exception {
        int size = 50;

        EC2Method createVolumeMock = mock(EC2Method.class);
        when(createVolumeMock.invoke()).thenReturn(resource("create_volume.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("Size", Integer.toString(size)), hasEntry("AvailabilityZone", DATA_CENTER),
                        hasEntry("Action", "CreateVolume")))).thenReturn(createVolumeMock);

        String returnedVolumeId = ebsVolume.createVolume(
                VolumeCreateOptions.getInstance(new Storage<Gigabyte>(size, Storage.GIGABYTE), null, null)
                        .inDataCenter(DATA_CENTER));

        verify(createVolumeMock, times(1)).invoke();

        assertEquals("vol-1a2b3c4d", returnedVolumeId);
    }

    @Test
    public void testCreateVolumeWithTags() throws Exception {
        String volumeId = "vol-1a2b3c4d";
        int size = 50;
        String name = "v_name";
        String description = "v_description";

        EC2Method createVolumeMock = mock(EC2Method.class);
        when(createVolumeMock.invoke()).thenReturn(resource("create_volume.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("Size", Integer.toString(size)), hasEntry("AvailabilityZone", DATA_CENTER),
                        hasEntry("Action", "CreateVolume")))).thenReturn(createVolumeMock);

        EC2Method createTagMock = mock(EC2Method.class);
        when(createTagMock.invoke()).thenReturn(resource("create_tags.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq("ec2"), eq(awsCloudStub),
                argThat(allOf(hasEntry("Tag.1.Key", "Name"), hasEntry("Tag.1.Value", name),
                        hasEntry("Tag.2.Key", "Description"), hasEntry("Tag.2.Value", description),
                        hasEntry("ResourceId.1", volumeId), hasEntry("Action", "CreateTags"))))
                .thenReturn(createTagMock);


        String returnedVolumeId = ebsVolume.createVolume(
                VolumeCreateOptions.getInstance(new Storage<Gigabyte>(size, Storage.GIGABYTE), name, description)
                        .inDataCenter(DATA_CENTER));

        verify(createVolumeMock, times(1)).invoke();
        verify(createTagMock, times(1)).invoke();

        assertEquals("vol-1a2b3c4d", returnedVolumeId);
    }

    @Test
    public void testCreateVolumeWithSnapshotId() throws Exception {
        String volumeId = "vol-1a2b3c4d";
        int size = 50;
        String snapshotId = "snapshot-1a2b3c4d";

        EC2Method createVolumeMock = mock(EC2Method.class);
        when(createVolumeMock.invoke()).thenReturn(resource("create_volume.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("Size", Integer.toString(size)), hasEntry("AvailabilityZone", DATA_CENTER),
                        hasEntry("Action", "CreateVolume"), hasEntry("SnapshotId", snapshotId))))
                .thenReturn(createVolumeMock);

        VolumeCreateOptions createOptions = VolumeCreateOptions
                .getInstance(new Storage<Gigabyte>(size, Storage.GIGABYTE), null, null)
                .inDataCenter(DATA_CENTER);
        createOptions.setSnapshotId(snapshotId);

        String returnedVolumeId = ebsVolume.createVolume(createOptions);

        verify(createVolumeMock, times(1)).invoke();

        assertEquals("vol-1a2b3c4d", returnedVolumeId);
    }

    @Test
    public void testCreateVolumeWithProductId() throws Exception {
        int size = 50;
        String productId = "io1";
        int iops = 1000;

        EC2Method createVolumeMock = mock(EC2Method.class);
        when(createVolumeMock.invoke()).thenReturn(resource("create_volume.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("Size", Integer.toString(size)), hasEntry("AvailabilityZone", DATA_CENTER),
                        hasEntry("VolumeType", productId), hasEntry("Iops", Integer.toString(iops)),
                        hasEntry("Action", "CreateVolume")))).thenReturn(createVolumeMock);

        VolumeCreateOptions createOptions = VolumeCreateOptions
                .getInstance(new Storage<Gigabyte>(size, Storage.GIGABYTE), null, null)
                .inDataCenter(DATA_CENTER);
        createOptions.setVolumeProductId(productId);
        createOptions.setIops(iops);

        String returnedVolumeId = ebsVolume.createVolume(createOptions);

        verify(createVolumeMock, times(1)).invoke();

        assertEquals("vol-1a2b3c4d", returnedVolumeId);
    }

    @Test
    public void testCreateVolumeWithoutDataCenter() throws Exception {

    }
}
