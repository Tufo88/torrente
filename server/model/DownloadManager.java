package model;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import message.P2PReqMessage;
import utils.tData;

public class DownloadManager extends Thread { // se encarga de coordinar el proceso p2p

	private Map<OyenteCliente, ArrayList<tData>> _senders;

	private boolean _clientServerSet;
	private boolean _portSet;

	private InetAddress _clientServerIp;
	private int _clientServerPort;

	public DownloadManager(Map<OyenteCliente, ArrayList<tData>> ocs) {

		this._senders = ocs;

	}

	@Override
	public void run() {

		if (!this._clientServerSet || !this._portSet)
			return;

		// pedimos los archivos a todos los senders
		for (Entry<OyenteCliente, ArrayList<tData>> kv : _senders.entrySet()) {

			OyenteCliente oc = kv.getKey();

			P2PReqMessage pm = new P2PReqMessage(oc.getClientKey(), this._clientServerPort, this._clientServerIp,
					kv.getValue());

			oc.sendMessage(pm);

		}

	}

	public void setServerIp(InetAddress serverIp) {

		this._clientServerIp = serverIp;
		this._clientServerSet = true;

	}

	public void setPort(int port) {

		this._clientServerPort = port;
		this._portSet = true;

	}

}
