package de.fzi.cep.sepa.manager.matching;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.fzi.cep.sepa.commons.GenericTree;
import de.fzi.cep.sepa.manager.matching.v2.ElementVerification;
import de.fzi.cep.sepa.manager.matching.v2.mapping.MappingPropertyCalculator;
import de.fzi.cep.sepa.manager.util.PipelineVerificationUtils;
import de.fzi.cep.sepa.manager.util.TreeUtils;
import de.fzi.cep.sepa.messages.PipelineModification;
import de.fzi.cep.sepa.messages.PipelineModificationMessage;
import de.fzi.cep.sepa.model.InvocableSEPAElement;
import de.fzi.cep.sepa.model.NamedSEPAElement;
import de.fzi.cep.sepa.model.client.Pipeline;
import de.fzi.cep.sepa.model.client.connection.Connection;
import de.fzi.cep.sepa.model.client.exception.InvalidConnectionException;

import de.fzi.cep.sepa.model.impl.EventStream;
import de.fzi.cep.sepa.model.impl.eventproperty.EventProperty;
import de.fzi.cep.sepa.model.impl.eventproperty.EventPropertyList;
import de.fzi.cep.sepa.model.impl.eventproperty.EventPropertyNested;
import de.fzi.cep.sepa.model.impl.eventproperty.EventPropertyPrimitive;
import de.fzi.cep.sepa.model.impl.graph.SecInvocation;
import de.fzi.cep.sepa.model.impl.graph.SepaInvocation;
import de.fzi.cep.sepa.model.impl.output.CustomOutputStrategy;
import de.fzi.cep.sepa.model.impl.output.ReplaceOutputStrategy;
import de.fzi.cep.sepa.model.impl.output.UriPropertyMapping;
import de.fzi.cep.sepa.model.impl.staticproperty.*;
import de.fzi.cep.sepa.storage.controller.StorageManager;


public class PipelineVerificationHandler {

    Pipeline pipeline;
    PipelineModificationMessage pipelineModificationMessage;

    List<InvocableSEPAElement> invocationGraphs;

    InvocableSEPAElement rdfRootElement;
    GenericTree<NamedSEPAElement> rdfTree;

    public PipelineVerificationHandler(Pipeline pipeline, boolean isPartial)
            throws Exception {

        this.pipeline = pipeline;
        this.rdfRootElement = PipelineVerificationUtils.getRootNode(pipeline);
        this.invocationGraphs = new ArrayList<>();

        // prepare a list of all pipeline elements without the root element
        List<NamedSEPAElement> sepaElements = new ArrayList<NamedSEPAElement>();
        sepaElements.addAll(pipeline.getSepas());
        sepaElements.addAll(pipeline.getStreams());
        sepaElements.add(pipeline.getAction());
        sepaElements.remove(rdfRootElement);

        // we need a tree of invocation graphs if there is more than one SEPA
        rdfTree = new TreeBuilder(pipeline, rdfRootElement).generateTree(true);

        pipelineModificationMessage = new PipelineModificationMessage();
    }

    public PipelineVerificationHandler validateConnection() throws InvalidConnectionException {

        ElementVerification verifier = new ElementVerification();
        boolean verified = true;
        // current root element can be either an action or a SEPA
        InvocableSEPAElement rightElement = rdfRootElement;
        List<String> connectedTo = rdfRootElement.getConnectedTo();

        Iterator<String> it = connectedTo.iterator();

        while (it.hasNext()) {
            String domId = it.next();
            NamedSEPAElement element = TreeUtils.findSEPAElement(domId, pipeline.getSepas(), pipeline.getStreams());
            if (element instanceof EventStream) {
                EventStream leftEventStream = (EventStream) element;

                if (!(verifier.verify(leftEventStream, rightElement))) verified = false;
            } else {
                invocationGraphs.addAll(makeInvocationGraphs(element));
                SepaInvocation ancestor = findInvocationGraph(invocationGraphs, element.getDOM());
                if (!(verifier.verify(ancestor, rightElement))) verified = false;
            }
        }

        if (!verified) throw new InvalidConnectionException(verifier.getErrorLog());
        return this;
    }

    /**
     * dummy method to compute mapping properties (based on EXACT input/output
     * matching)
     *
     * @return PipelineValidationHandler
     */

    public PipelineVerificationHandler computeMappingProperties() {
        try {
            List<String> connectedTo = rdfRootElement.getConnectedTo();
            String domId = rdfRootElement.getDOM();


            for (int i = 0; i < connectedTo.size(); i++) {
                NamedSEPAElement element = TreeUtils.findSEPAElement(rdfRootElement
                        .getConnectedTo().get(i), pipeline.getSepas(), pipeline
                        .getStreams());

                if (element instanceof SepaInvocation || element instanceof EventStream) {

                    if (element instanceof SepaInvocation) {

                        SepaInvocation ancestor = (SepaInvocation) TreeUtils.findByDomId(
                                connectedTo.get(i), invocationGraphs);

                        updateStaticProperties(ancestor.getOutputStream(), i);
                        updateOutputStrategy(ancestor.getOutputStream(), i);

                    } else if (element instanceof EventStream) {

                        EventStream stream = (EventStream) element;

                        updateStaticProperties(stream, i);
                        updateOutputStrategy(stream, i);

                    }

                    if (rdfRootElement.getStreamRequirements().size() - 1 == i) {
                        PipelineModification modification = new PipelineModification(
                                domId, rdfRootElement.getElementId(), rdfRootElement.getStaticProperties());
                        if (rdfRootElement instanceof SepaInvocation)
                            modification.setOutputStrategies(((SepaInvocation) rdfRootElement).getOutputStrategies());
                        pipelineModificationMessage
                                .addPipelineModification(modification);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    private void updateStaticProperties(EventStream stream, Integer count) {

        rdfRootElement
                .getStaticProperties()
                .stream()
                .filter(property -> property instanceof MappingProperty)
                .forEach(property -> {
                    try {
                        MappingProperty mappingProperty = (MappingProperty) property;
                        mappingProperty.setMapsFromOptions(new ArrayList<>());

                        if (mappingProperty.getMapsFrom() != null) {
                            ((MappingProperty) property)
                                    .setMapsFromOptions(findSupportedEventProperties(stream,
                                            rdfRootElement.getStreamRequirements(),
                                            mappingProperty.getMapsFrom()));
                        } else {
                            for (EventProperty streamProperty : stream
                                    .getEventSchema().getEventProperties()) {

                                if ((streamProperty instanceof EventPropertyPrimitive) || streamProperty instanceof EventPropertyList) {
                                    mappingProperty.getMapsFromOptions().add(streamProperty);
                                } else {
                                    mappingProperty.getMapsFromOptions().addAll(addNestedProperties((EventPropertyNested) streamProperty));
                                }
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private List<EventProperty> findSupportedEventProperties(EventStream streamOffer, List<EventStream> streamRequirements, URI mapsFrom) {
        EventProperty mapsFromProperty = TreeUtils
                .findEventProperty(mapsFrom.toString(), rdfRootElement.getStreamRequirements());

        return new MappingPropertyCalculator().matchesProperties(streamOffer.getEventSchema().getEventProperties(), mapsFromProperty);
    }

    private void updateOutputStrategy(EventStream stream, Integer count) {

        if (rdfRootElement instanceof SepaInvocation) {
            ((SepaInvocation) rdfRootElement)
                    .getOutputStrategies()
                    .stream()
                    .filter(strategy -> strategy instanceof CustomOutputStrategy)
                    .forEach(strategy -> {
                        CustomOutputStrategy outputStrategy = (CustomOutputStrategy) strategy;
                        if (count == 0) {
                            outputStrategy.setProvidesProperties(new ArrayList<>());
                        }
                        if (outputStrategy.isOutputRight() && count > 0)
                            outputStrategy.setProvidesProperties(stream.getEventSchema().getEventProperties());
                        else {
                            if (outputStrategy.getProvidesProperties() == null) {
                                outputStrategy.setProvidesProperties(new ArrayList<>());
                            }
                            outputStrategy.getProvidesProperties().addAll(stream.getEventSchema().getEventProperties());
                        }
                    });

            ((SepaInvocation) rdfRootElement)
                    .getOutputStrategies()
                    .stream()
                    .filter(strategy -> strategy instanceof ReplaceOutputStrategy)
                    .forEach(strategy -> {
                        ReplaceOutputStrategy outputStrategy = (ReplaceOutputStrategy) strategy;

                        for (UriPropertyMapping mapping : outputStrategy.getReplaceProperties()) {
                            if (mapping.getReplaceFrom() != null) {
                                mapping.setReplaceWithOptions(findSupportedEventProperties(stream, rdfRootElement.getStreamRequirements(), mapping.getReplaceFrom()));
                            } else {
                                mapping.setReplaceWithOptions(stream.getEventSchema().getEventProperties());
                            }
                        }
                    });
        }
    }

    private List<EventProperty> addNestedProperties(EventPropertyNested properties) {
        List<EventProperty> options = new ArrayList<>();
        for (EventProperty p : properties.getEventProperties()) {
            if (p instanceof EventPropertyPrimitive) options.add(p);
            else options.addAll(addNestedProperties(properties));
        }
        return options;
    }

    public PipelineVerificationHandler storeConnection() {
        String fromId = rdfRootElement.getConnectedTo().get(rdfRootElement.getConnectedTo().size() - 1);
        NamedSEPAElement sepaElement = TreeUtils.findSEPAElement(fromId, pipeline.getSepas(), pipeline.getStreams());

        Connection connection = new Connection(sepaElement.getElementId(), rdfRootElement.getBelongsTo());
        StorageManager.INSTANCE.getConnectionStorageApi().addConnection(connection);
        return this;
    }

    public PipelineModificationMessage getPipelineModificationMessage() {
        return pipelineModificationMessage;
    }


    private List<InvocableSEPAElement> makeInvocationGraphs(NamedSEPAElement rootElement) {
        GenericTree<NamedSEPAElement> tree = new TreeBuilder(
                pipeline, rootElement).generateTree(true);
        return new InvocationGraphBuilder(tree, true, null).buildGraph();
    }

    private SepaInvocation findInvocationGraph(List<InvocableSEPAElement> graphs, String domId) {
        return (SepaInvocation) TreeUtils.findByDomId(domId, graphs);
    }

}