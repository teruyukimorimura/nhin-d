﻿/* 
 Copyright (c) 2010, Direct Project
 All rights reserved.

 Authors:
    Umesh Madan     umeshma@microsoft.com
    John Theisen    jtheisen@kryptiq.com
  
Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
Neither the name of the The Direct Project (nhindirect.org). nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
*/
using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Mail;
using System.Security.Cryptography.X509Certificates;

using Health.Direct.Common.Certificates;

using Xunit;
using Xunit.Extensions;

namespace Health.Direct.Agent.Tests
{
    public class DnsResolverTest
    {
        public const string ServerIP = "127.0.0.1";
        
        public static IEnumerable<object[]> GoodAddresses
        {
            get
            {
                yield return new[] {"bob@nhind.hsgincubator.com"};
                yield return new[] {"biff@nhind.hsgincubator.com"};
                yield return new[] {"gatewaytest@hotmail.com"};
            }
        }
        
        [Theory(Skip = "Requires Bind Server to be running on the local server")]
        [PropertyData("GoodAddresses")]
        public void GetCertificateWithGoodAddress(string address)
        {
            var resolver = new DnsCertResolver(IPAddress.Parse(ServerIP)
                                               , TimeSpan.FromSeconds(5)
                                               , "hsgincubator.com"
                                               , false);
            X509Certificate2Collection certs = resolver.GetCertificates(new MailAddress(address));
            Assert.True(certs.Count > 0);
        }
    }
}