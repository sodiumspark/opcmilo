package milo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.toList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class Client  {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static void main( String[] args )
	{

		new Client();

		List<EndpointDescription> endpoints = getEndpointDescriptions();

		OpcUaClientConfigBuilder cfg = new OpcUaClientConfigBuilder();
		cfg.setEndpoint(endpoints.get(0)); 

		OpcUaClient client = null;

		try {
			client = OpcUaClient.create(cfg.build());

			client.connect().get();
		} catch (UaException | InterruptedException | ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("Connected");

		browseNode("", client, Identifiers.RootFolder);

		Object temp = null;
		Object pressure = null;
		Object time = null;


		while(true) {
			try {
				temp= client.readValue(0, null, new NodeId(2, 2)).get().getValue().getValue();
				pressure= client.readValue(0, null, new NodeId(2, 3)).get().getValue().getValue();
				time= client.readValue(0, null, new NodeId(2, 4)).get().getValue().getValue();
			}
			catch (Exception e) {
				System.out.println("Waiting");
			}   
			System.out.println("Temprature : "+temp + "  Pressure : "+ pressure + "  Time :  "+ time);

			try {
				Thread.sleep(11);
			} catch (InterruptedException e) {
				System.out.println("Sleep exception, better rethrow");
			}
		}

	}  



	
	private static void browseNode(String indent, OpcUaClient client, NodeId browseRoot) {

		BrowseDescription browse = new BrowseDescription(
				browseRoot,
				BrowseDirection.Forward,
				Identifiers.References,
				true,
				uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue()),
				uint(BrowseResultMask.All.getValue())
				);

		try {
			BrowseResult browseResult = client.browse(browse).get();

			List<ReferenceDescription> references = toList(browseResult.getReferences());

			for (ReferenceDescription rd : references) {
				System.out.println("{} Node={}"+ indent + rd.getBrowseName().getName());

				// recursively browse to children
				rd.getNodeId().local().ifPresent(nodeId -> browseNode(indent + "  ", client, nodeId));
			}
		} catch (Exception e) {
			System.out.println("Failed Browsing");
		}
	}

	private static List<EndpointDescription>  getEndpointDescriptions()  {

		List<EndpointDescription> endpoints = null;

		try {
			endpoints =
					DiscoveryClient.getEndpoints("opc.tcp://localhost:4841")
					.get();
			System.out.println(endpoints.toString());
			System.out.println(endpoints.size());

		} catch (Exception ex) {
			System.out.println("Server not up");


		}
		return endpoints;



	}


	@SuppressWarnings("unused")
	private CompletableFuture<List<DataValue>> readServerStateAndTime(OpcUaClient client) {
		List<NodeId> nodeIds = ImmutableList.of(
				Identifiers.Server_ServerStatus_State,
				Identifiers.Server_ServerStatus_CurrentTime);

		return client.readValues(0.0, TimestampsToReturn.Both, nodeIds);
	}


}
