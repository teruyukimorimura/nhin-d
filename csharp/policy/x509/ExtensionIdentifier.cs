﻿using System;
using System.Collections.Generic;
using System.Linq.Expressions;
using Health.Direct.Policy.Extensions;

namespace Health.Direct.Policy.X509
{
    public class ExtensionIdentifier<T>
    {

        static ExtensionIdentifier() 
        {
            var extensionfield = Expression.Parameter(typeof (IExtensionField<T>), "extensionField");
        }
        /**
	     * Key usage extension.  Defined by RFC5280 section 4.2.1.3
	     */

       public static readonly ExtensionIdentifier<T> KeyUsage
            = new ExtensionIdentifier<T>("2.5.29.15", "KeyUsage", "Key Usage"
                , typeof(IExtensionField<T>).Ctor<bool, IExtensionField<T>>());

        readonly string m_id;
        readonly string m_rfcName;
        readonly string m_display;
        readonly IList<AttributeReferenceClass<T>> m_subAttributes;
        Func<bool, IExtensionField<T>> m_referenceClass;
        static Dictionary<String, ExtensionIdentifier<T>> tokenFieldMap; 

        private ExtensionIdentifier(string id, String rfcName, String display, Func<bool, IExtensionField<T>> referenceClass)
        {
            m_id = id;
		    m_rfcName = rfcName;
		    m_display = display;
		    m_subAttributes = null;
		    m_referenceClass = referenceClass;		
	    }

        
        /// <summary>
        /// Gets the object identifier (OID) of the extension.
        /// </summary>
        /// <returns>The object identifier (OID) of the extension.</returns>
        public string GetId()
        {
            return m_id;
        }

        /// <summary>
	    /// Gets the name of the extension as defined by RFC5280.
	    /// @return The name of the extension as defined by RFC5280.
	    /// </summary>
	    public String GetRfcName()
	    {
		    return m_rfcName;
	    }
	
	    /// <summary>
	    /// Gets a human readable display name of the extension.
	    /// <returns>Human readable display name of the extension.</returns>
	    /// </summary>
	    public String GetDisplay()
	    {
		    return m_display;
	    }
	
        

        /// <summary>
	    /// Gets the class implementing the extension.
	    /// <returns>
	    /// The Type implementing the extension.
	    /// </returns> 
	    /// </summary>
	    public IExtensionField<T> GetReferenceClass(String tokenName) 
	    {
		    IExtensionField<T> retVal = null;
		
		    if (m_referenceClass != null)
			    return m_referenceClass as IExtensionField<T>;
		    else
		    {
			    int idx = tokenName.LastIndexOf(".");
			    if (idx >= 0)
			    {
				    string name = tokenName.Substring(idx + 1);

                    foreach (var attrRef in m_subAttributes)
			        {
					    if (name.Equals(attrRef.GetAttribute()))
					    {
						    retVal = attrRef.GetReferenceClass();
						    break;
					    }		
				    }
			    }
		    }
		
		    return retVal;
	    }

        /// <summary>
        /// Gets the extension associated with a specific <paramref name="token"/> string.
        /// <param name="token"/>Used to look up the ExtensionIdentifier.
        /// <returns>The ExtensionIdentifier associated with the token.  If the token does not represent a known extension, then null is returned.</returns>
        /// </summary>
        public static ExtensionIdentifier<T> FromToken(String token)
        {
            ExtensionIdentifier<T> extensionIdentifier;
            tokenFieldMap.TryGetValue(token, out extensionIdentifier);
            return extensionIdentifier;
        }
    }

    //Todo: combine with TBSFieldName.AttributeReferenceClass ???
        public class AttributeReferenceClass<T>
        {
            private readonly String attribute;
            //protected final Class<? extends TBSField<?>> referenceClass;
            private IExtensionField<T> referenceClass;

            //public AttributeReferenceClass(String attribute, Class<? extends TBSField<?>> referenceClass)
            //{
            //    this.attribute = attribute;
            //    this.referenceClass = referenceClass;
            //}

            public AttributeReferenceClass(String attribute, IExtensionField<T> referenceClass)
            {
                this.attribute = attribute;
                this.referenceClass = referenceClass;
            }


            public String GetAttribute()
            {
                return attribute;
            }

            //public Class<? extends TBSField<?>> getReferenceClass()
            //{
            //    return referenceClass;

            //}

            public IExtensionField<T> GetReferenceClass()
            {
                return referenceClass;
            }
        }

}
