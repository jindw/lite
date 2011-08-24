function DOMImplementation() {
  //this._preserveWhiteSpace = false;  // by default, ignore whitespace
  //this._namespaceAware = true;       // by default, handle namespaces
  //this._errorChecking  = true;       // by default, test for exceptions
};
DOMImplementation.prototype.hasFeature = function DOMImplementation_hasFeature(feature, version) {
  var ret = false;
  if (feature.toLowerCase() == "xml") {
    ret = (!version || (version == "1.0") || (version == "2.0"));
  }
  else if (feature.toLowerCase() == "core") {
    ret = (!version || (version == "2.0"));
  }
  return ret;
};
DOMImplementation.prototype.createDocumentType = function(qualifiedName, 
                                           publicId, 
                                           systemId){
	
}
DOMImplementation.prototype.createDocument = function(namespaceURI, 
                                   qualifiedName, 
                                   doctype){
	
}
