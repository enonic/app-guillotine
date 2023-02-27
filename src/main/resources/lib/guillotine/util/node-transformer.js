function addRecursiveNodeId(holder, nodeId, searchTarget) {
    if (typeof holder === 'object') {
        holder['__nodeId'] = nodeId;
        holder['__searchTarget'] = searchTarget;

        Object.keys(holder).forEach(prop => {
            const holderElement = holder[prop];
            if (prop !== '__searchTarget' && typeof holderElement === 'object') {
                if (Array.isArray(holderElement)) {
                    holderElement.forEach(p => addRecursiveNodeId(p, nodeId, searchTarget));
                } else {
                    holderElement['__nodeId'] = nodeId;
                    holderElement['__searchTarget'] = searchTarget;
                    addRecursiveNodeId(holderElement, nodeId, searchTarget);
                }
            }
        });
    }
}

function removeSystemPropertiesFrom(obj) {
    if (typeof obj === 'object') {
        delete obj['__nodeId'];
        delete obj['__searchTarget'];
        Object.keys(obj).forEach(prop => {
            const holderProp = obj[prop];
            if (typeof holderProp === 'object') {
                if (Array.isArray(holderProp)) {
                    holderProp.forEach(p => removeSystemPropertiesFrom(p));
                } else {
                    removeSystemPropertiesFrom(holderProp);
                }
            }
        });
    }
}

exports.addRecursiveNodeId = addRecursiveNodeId;
exports.removeNodeIdPropIfNeeded = removeSystemPropertiesFrom;
exports.removeSystemPropertiesFrom = removeSystemPropertiesFrom;
